package com.testings.ddas.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import com.testings.ddas.Model.FileMetadata;

@Component
public class DirectoryWatcher {

    @Autowired
    private FileMetadataService fileMetadataService;

    private final Map<WatchKey, Path> watchKeyPathMap = new HashMap<>();

    public void watchDirectory(Path startPath) throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        registerAll(startPath, watchService);

        while (true) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                return;
            }

            Path dir = watchKeyPathMap.get(key);
            if (dir == null) {
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path fileName = ev.context();
                Path child = dir.resolve(fileName);

                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    if (Files.isDirectory(child)) {
                        try {
                            registerAll(child, watchService);
                        } catch (Exception e) {
                            System.err.println("Access denied to directory: " + child + " - Skipping.");
                        }
                    } else {
                        processNewFile(child);
                    }
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    processDeletedFile(child, fileName.toString());
                }
            }

            if (!key.reset()) {
                watchKeyPathMap.remove(key);
                if (watchKeyPathMap.isEmpty()) {
                    break;
                }
            }
        }
    }

    private void registerAll(final Path start, final WatchService watchService) {
        try {
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (isSystemDirectory(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    try {
                        register(dir, watchService);
                    } catch (AccessDeniedException e) {
                        System.err.println("Access denied to directory: " + dir + " - Skipping.");
                        return FileVisitResult.SKIP_SUBTREE; // Skip this directory
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        // You can add file processing logic here if needed
                    } catch (Exception e) {
                        System.err.println("Access denied to file: " + file + " - Skipping.");
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Error while registering directories: " + e.getMessage());
        }
    }


    private boolean isSystemDirectory(Path dir) {
        Path fileName = dir.getFileName();
        if (fileName == null) {
            return false; // Return false for root directories or any path with no file name
        }
        String dirName = fileName.toString();
        return dirName.equals("$RECYCLE.BIN") || dirName.startsWith("$") || dirName.equals("System Volume Information");
    }

    private void register(Path dir, WatchService watchService) throws IOException {
        WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
        watchKeyPathMap.put(key, dir);
    }

    private void processNewFile(Path filePath) {
        try {
            if (Files.isRegularFile(filePath)) {
                FileMetadata fileMetadata = new FileMetadata();
                fileMetadata.setFileName(filePath.getFileName().toString());
                fileMetadata.setFileDirectory(filePath.getParent().toString());
                fileMetadata.setFileSize(Files.size(filePath));
                fileMetadata.setLastModified(LocalDateTime.ofInstant(Files.getLastModifiedTime(filePath).toInstant(), ZoneId.systemDefault()));
                fileMetadata.setContentType(Files.probeContentType(filePath));
                fileMetadata.setFileHash(HashGenerator.generateHash(filePath));

                fileMetadataService.saveFileMetadata(fileMetadata);
                System.out.println("New file processed: " + fileMetadata.getFileName());
            }
        } catch (Exception e) {
            System.err.println("Unable to process new file: " + filePath + " due to " + e.getMessage());
        }
    }

    private void processDeletedFile(Path filePath, String filename) {
        try {
            fileMetadataService.deleteFileMetadataByPath(Path.of(filePath.toString()), filename);
            System.out.println("File deleted and metadata removed: " + filePath);
        } catch (Exception e) {
            System.err.println("Unable to process deleted file: " + filePath + " due to " + e.getMessage());
        }
    }
}