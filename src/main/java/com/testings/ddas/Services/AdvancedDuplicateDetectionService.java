package com.testings.ddas.Services;

import com.testings.ddas.config.DdasProperties;
import com.testings.ddas.Model.FileMetadata;
import com.testings.ddas.Repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class AdvancedDuplicateDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedDuplicateDetectionService.class);

    @Autowired
    private DdasProperties properties;

    @Autowired
    private FileMetadataRepository repository;

    @Autowired
    private FileMetadataService fileMetadataService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ExecutorService duplicateProcessor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    );

    private final AtomicLong duplicatesDetected = new AtomicLong(0);
    private final AtomicLong duplicatesDeleted = new AtomicLong(0);
    private final AtomicLong spaceReclaimed = new AtomicLong(0);

    @jakarta.annotation.PostConstruct
    private void initialize() {
        if (properties.getDuplicateDetection().isAutoDelete()) {
            // Schedule periodic duplicate detection
            scheduler.scheduleWithFixedDelay(
                this::performScheduledDuplicateDetection,
                properties.getDuplicateDetection().getDuplicateCheckDelay().toSeconds(),
                properties.getDuplicateDetection().getDuplicateCheckDelay().toSeconds(),
                TimeUnit.SECONDS
            );
            logger.info("Scheduled duplicate detection every {} seconds", 
                       properties.getDuplicateDetection().getDuplicateCheckDelay().toSeconds());
        }
    }

    /**
     * Detects and processes duplicates for a specific file
     */
    @Async
    public CompletableFuture<DuplicateDetectionResult> processFileForDuplicates(FileMetadata newFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (newFile.getFileSize() < properties.getDuplicateDetection().getMinFileSizeForDuplication()) {
                    return new DuplicateDetectionResult(false, 0, 0, "File too small for duplicate detection");
                }

                // Skip protected file extensions
                if (isProtectedFile(newFile)) {
                    return new DuplicateDetectionResult(false, 0, 0, "Protected file extension");
                }

                List<FileMetadata> duplicates = findDuplicates(newFile);
                
                if (duplicates.isEmpty()) {
                    return new DuplicateDetectionResult(false, 0, 0, "No duplicates found");
                }

                duplicatesDetected.addAndGet(duplicates.size());

                if (properties.getDuplicateDetection().isAutoDelete()) {
                    DeletionResult deletionResult = processDuplicatesForDeletion(newFile, duplicates);
                    return new DuplicateDetectionResult(
                        true, 
                        duplicates.size(), 
                        deletionResult.getDeletedCount(),
                        deletionResult.getMessage()
                    );
                } else {
                    // Just log the duplicates found
                    logger.info("Found {} duplicates for file: {} (auto-delete disabled)", 
                               duplicates.size(), newFile.getFileName());
                    return new DuplicateDetectionResult(true, duplicates.size(), 0, "Duplicates found but not deleted");
                }

            } catch (Exception e) {
                logger.error("Error processing file for duplicates: {}", newFile.getFileName(), e);
                return new DuplicateDetectionResult(false, 0, 0, "Error: " + e.getMessage());
            }
        }, duplicateProcessor);
    }

    /**
     * Batch duplicate detection and processing
     */
    @Transactional
    public CompletableFuture<BatchDuplicateResult> processBatchDuplicates(List<FileMetadata> files) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            int totalProcessed = 0;
            int totalDuplicatesFound = 0;
            int totalDeleted = 0;

            // Group files by hash for efficient processing
            Map<String, List<FileMetadata>> hashGroups = files.stream()
                .filter(f -> f.getFileHash() != null && !f.getFileHash().isEmpty())
                .filter(f -> f.getFileSize() >= properties.getDuplicateDetection().getMinFileSizeForDuplication())
                .filter(f -> !isProtectedFile(f))
                .collect(Collectors.groupingBy(FileMetadata::getFileHash));

            for (Map.Entry<String, List<FileMetadata>> entry : hashGroups.entrySet()) {
                List<FileMetadata> filesWithSameHash = entry.getValue();
                
                if (filesWithSameHash.size() > 1) {
                    // Found duplicates
                    totalDuplicatesFound += filesWithSameHash.size() - 1; // Subtract 1 as we keep one copy
                    
                    if (properties.getDuplicateDetection().isAutoDelete()) {
                        DeletionResult deletionResult = processDuplicatesForDeletion(filesWithSameHash);
                        totalDeleted += deletionResult.getDeletedCount();
                    }
                }
                totalProcessed += filesWithSameHash.size();
            }

            long duration = System.currentTimeMillis() - startTime;
            
            logger.info("Batch duplicate processing completed: {} files processed, {} duplicates found, {} deleted in {}ms",
                       totalProcessed, totalDuplicatesFound, totalDeleted, duration);

            return new BatchDuplicateResult(totalProcessed, totalDuplicatesFound, totalDeleted, duration);
        }, duplicateProcessor);
    }

    /**
     * Finds duplicate files based on hash
     */
    private List<FileMetadata> findDuplicates(FileMetadata file) {
        if (file.getFileHash() == null || file.getFileHash().isEmpty()) {
            return Collections.emptyList();
        }

        return repository.findByFileHashAndIdNot(file.getFileHash(), file.getId() != null ? file.getId() : -1L);
    }

    /**
     * Determines which duplicates to delete based on configured strategy
     */
    private DeletionResult processDuplicatesForDeletion(FileMetadata newFile, List<FileMetadata> duplicates) {
        List<FileMetadata> allFiles = new ArrayList<>(duplicates);
        allFiles.add(newFile);
        
        return processDuplicatesForDeletion(allFiles);
    }

    private DeletionResult processDuplicatesForDeletion(List<FileMetadata> duplicates) {
        if (duplicates.size() <= 1) {
            return new DeletionResult(0, 0, "No duplicates to delete");
        }

        // Sort files based on deletion strategy to determine which to keep
        FileMetadata fileToKeep = selectFileToKeep(duplicates);
        List<FileMetadata> filesToDelete = duplicates.stream()
            .filter(f -> !f.equals(fileToKeep))
            .collect(Collectors.toList());

        int deletedCount = 0;
        long spaceFreed = 0;

        for (FileMetadata fileToDelete : filesToDelete) {
            try {
                Path filePath = Paths.get(fileToDelete.getFileDirectory(), fileToDelete.getFileName());
                
                if (Files.exists(filePath)) {
                    long fileSize = fileToDelete.getFileSize();
                    Files.delete(filePath);
                    
                    // Remove from database
                    repository.delete(fileToDelete);
                    
                    deletedCount++;
                    spaceFreed += fileSize;
                    
                    if (properties.getDuplicateDetection().isEnableDeduplicationLogging()) {
                        logger.info("Deleted duplicate file: {} (freed {} bytes)", filePath, fileSize);
                    }
                } else {
                    // File doesn't exist, just remove from database
                    repository.delete(fileToDelete);
                    logger.warn("File not found for deletion, removed from database: {}", 
                               Paths.get(fileToDelete.getFileDirectory(), fileToDelete.getFileName()));
                }
            } catch (Exception e) {
                logger.error("Failed to delete duplicate file: {}/{}", 
                           fileToDelete.getFileDirectory(), fileToDelete.getFileName(), e);
            }
        }

        duplicatesDeleted.addAndGet(deletedCount);
        spaceReclaimed.addAndGet(spaceFreed);

        String message = String.format("Deleted %d duplicates, freed %d bytes, kept: %s", 
                                     deletedCount, spaceFreed, fileToKeep.getFileName());
        
        return new DeletionResult(deletedCount, spaceFreed, message);
    }

    /**
     * Selects which file to keep based on configured deletion strategy
     */
    private FileMetadata selectFileToKeep(List<FileMetadata> duplicates) {
        var strategy = properties.getDuplicateDetection().getDeletionStrategy();
        
        return switch (strategy) {
            case KEEP_OLDEST -> duplicates.stream()
                .min(Comparator.comparing(FileMetadata::getLastModified))
                .orElse(duplicates.get(0));
                
            case KEEP_NEWEST -> duplicates.stream()
                .max(Comparator.comparing(FileMetadata::getLastModified))
                .orElse(duplicates.get(0));
                
            case KEEP_SMALLEST -> duplicates.stream()
                .min(Comparator.comparing(FileMetadata::getFileSize))
                .orElse(duplicates.get(0));
                
            case KEEP_LARGEST -> duplicates.stream()
                .max(Comparator.comparing(FileMetadata::getFileSize))
                .orElse(duplicates.get(0));
                
            case KEEP_FIRST_FOUND -> duplicates.get(0);
        };
    }

    /**
     * Checks if file has a protected extension
     */
    private boolean isProtectedFile(FileMetadata file) {
        String fileName = file.getFileName().toLowerCase();
        return properties.getDuplicateDetection().getProtectedExtensions().stream()
            .anyMatch(fileName::endsWith);
    }

    /**
     * Scheduled duplicate detection for existing files
     */
    private void performScheduledDuplicateDetection() {
        try {
            logger.info("Starting scheduled duplicate detection...");
            
            // Find files with duplicate hashes
            List<String> duplicateHashes = repository.findDuplicateHashes(
                properties.getDuplicateDetection().getMinFileSizeForDuplication()
            );

            if (duplicateHashes.isEmpty()) {
                logger.info("No duplicates found during scheduled check");
                return;
            }

            logger.info("Found {} hash groups with duplicates", duplicateHashes.size());

            for (String hash : duplicateHashes) {
                List<FileMetadata> duplicates = repository.findByFileHash(hash);
                if (duplicates.size() > 1) {
                    processDuplicatesForDeletion(duplicates);
                }
            }

        } catch (Exception e) {
            logger.error("Error during scheduled duplicate detection", e);
        }
    }

    /**
     * Gets duplicate detection statistics
     */
    public DuplicateStatistics getStatistics() {
        long totalFiles = repository.count();
        long duplicateHashCount = repository.countDuplicateHashes(
            properties.getDuplicateDetection().getMinFileSizeForDuplication()
        );

        return new DuplicateStatistics(
            duplicatesDetected.get(),
            duplicatesDeleted.get(),
            spaceReclaimed.get(),
            totalFiles,
            duplicateHashCount,
            properties.getDuplicateDetection().isAutoDelete()
        );
    }

    @jakarta.annotation.PreDestroy
    public void shutdown() {
        logger.info("Shutting down AdvancedDuplicateDetectionService...");
        
        scheduler.shutdown();
        duplicateProcessor.shutdown();
        
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!duplicateProcessor.awaitTermination(30, TimeUnit.SECONDS)) {
                duplicateProcessor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            duplicateProcessor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Data classes for results and statistics
    public static class DuplicateDetectionResult {
        private final boolean duplicatesFound;
        private final int duplicateCount;
        private final int deletedCount;
        private final String message;

        public DuplicateDetectionResult(boolean duplicatesFound, int duplicateCount, int deletedCount, String message) {
            this.duplicatesFound = duplicatesFound;
            this.duplicateCount = duplicateCount;
            this.deletedCount = deletedCount;
            this.message = message;
        }

        // Getters
        public boolean isDuplicatesFound() { return duplicatesFound; }
        public int getDuplicateCount() { return duplicateCount; }
        public int getDeletedCount() { return deletedCount; }
        public String getMessage() { return message; }
    }

    public static class BatchDuplicateResult {
        private final int processedCount;
        private final int duplicatesFound;
        private final int deletedCount;
        private final long durationMs;

        public BatchDuplicateResult(int processedCount, int duplicatesFound, int deletedCount, long durationMs) {
            this.processedCount = processedCount;
            this.duplicatesFound = duplicatesFound;
            this.deletedCount = deletedCount;
            this.durationMs = durationMs;
        }

        // Getters
        public int getProcessedCount() { return processedCount; }
        public int getDuplicatesFound() { return duplicatesFound; }
        public int getDeletedCount() { return deletedCount; }
        public long getDurationMs() { return durationMs; }
    }

    public static class DeletionResult {
        private final int deletedCount;
        private final long spaceFreed;
        private final String message;

        public DeletionResult(int deletedCount, long spaceFreed, String message) {
            this.deletedCount = deletedCount;
            this.spaceFreed = spaceFreed;
            this.message = message;
        }

        // Getters
        public int getDeletedCount() { return deletedCount; }
        public long getSpaceFreed() { return spaceFreed; }
        public String getMessage() { return message; }
    }

    public static class DuplicateStatistics {
        private final long duplicatesDetected;
        private final long duplicatesDeleted;
        private final long spaceReclaimed;
        private final long totalFiles;
        private final long duplicateHashGroups;
        private final boolean autoDeleteEnabled;

        public DuplicateStatistics(long duplicatesDetected, long duplicatesDeleted, long spaceReclaimed, 
                                 long totalFiles, long duplicateHashGroups, boolean autoDeleteEnabled) {
            this.duplicatesDetected = duplicatesDetected;
            this.duplicatesDeleted = duplicatesDeleted;
            this.spaceReclaimed = spaceReclaimed;
            this.totalFiles = totalFiles;
            this.duplicateHashGroups = duplicateHashGroups;
            this.autoDeleteEnabled = autoDeleteEnabled;
        }

        // Getters
        public long getDuplicatesDetected() { return duplicatesDetected; }
        public long getDuplicatesDeleted() { return duplicatesDeleted; }
        public long getSpaceReclaimed() { return spaceReclaimed; }
        public long getTotalFiles() { return totalFiles; }
        public long getDuplicateHashGroups() { return duplicateHashGroups; }
        public boolean isAutoDeleteEnabled() { return autoDeleteEnabled; }
    }
}
