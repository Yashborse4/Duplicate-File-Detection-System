package com.testings.ddas.Services;

import com.testings.ddas.config.DdasProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Stream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import com.testings.ddas.Model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;

@Component
public class FileMetadataScanner {

    private static final Logger logger = LoggerFactory.getLogger(FileMetadataScanner.class);

    @Autowired
    private FileMetadataService fileMetadataService;

    @Autowired
    private DdasProperties properties;

    @Autowired
    private FileFilterService fileFilterService;

    private final ConcurrentHashMap<String, String> fileMap = new ConcurrentHashMap<>();
    private final AtomicLong processedFiles = new AtomicLong(0);
    private final AtomicLong skippedFiles = new AtomicLong(0);
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    
    private ForkJoinPool forkJoinPool;
    private ExecutorService hashingExecutor;
    private BlockingQueue<FileMetadata> metadataQueue;
    private volatile boolean isScanning = false;
    private final Semaphore scanSemaphore;

    public FileMetadataScanner() {
        this.scanSemaphore = new Semaphore(3); // Default max concurrent scans
    }

    @jakarta.annotation.PostConstruct
    private void initialize() {
        var processingConfig = properties.getProcessing();
        
        // Initialize ForkJoinPool with work-stealing
        if (processingConfig.isUseWorkStealing()) {
            forkJoinPool = new ForkJoinPool(processingConfig.getParallelism());
        } else {
            forkJoinPool = ForkJoinPool.commonPool();
        }
        
        // Initialize hashing executor with virtual threads if available
        if (processingConfig.isEnableVirtualThreads()) {
            try {
                hashingExecutor = (ExecutorService) Executors.class
                    .getMethod("newVirtualThreadPerTaskExecutor")
                    .invoke(null);
                logger.info("Using virtual threads for hashing operations");
            } catch (Exception e) {
                logger.warn("Virtual threads not available, falling back to regular threads");
                hashingExecutor = Executors.newFixedThreadPool(properties.getPerformance().getHashingThreads());
            }
        } else {
            hashingExecutor = Executors.newFixedThreadPool(properties.getPerformance().getHashingThreads());
        }
        
        metadataQueue = new LinkedBlockingQueue<>(processingConfig.getQueueCapacity());
        startMetadataSaver();
        
        logger.info("FileMetadataScanner initialized with {} parallelism, {} hashing threads", 
                   processingConfig.getParallelism(), properties.getPerformance().getHashingThreads());
    }

    public CompletableFuture<ScanResult> scanAllFiles() {
        if (properties.getMonitoredPaths().isEmpty()) {
            return scanDirectory("D:/");
        }
        
        List<CompletableFuture<ScanResult>> futures = properties.getMonitoredPaths().stream()
            .map(this::scanDirectory)
            .toList();
            
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .reduce(ScanResult::combine)
                .orElse(new ScanResult()));
    }

    public CompletableFuture<ScanResult> scanDirectory(String directoryPath) {
        return scanDirectory(Paths.get(directoryPath));
    }

    public CompletableFuture<ScanResult> scanDirectory(Path startPath) {
        if (!scanSemaphore.tryAcquire()) {
            return CompletableFuture.completedFuture(
                new ScanResult(0, 0, "Max concurrent scans reached"));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                isScanning = true;
                long startTime = System.currentTimeMillis();
                logger.info("Starting directory scan: {}", startPath);
                
                ScanResult result = performDirectoryScan(startPath);
                
                long duration = System.currentTimeMillis() - startTime;
                result.setDurationMs(duration);
                
                logger.info("Completed directory scan: {} in {}ms. Files processed: {}, skipped: {}", 
                           startPath, duration, result.getProcessedFiles(), result.getSkippedFiles());
                
                return result;
            } finally {
                isScanning = false;
                scanSemaphore.release();
            }
        }, forkJoinPool);
    }

    private ScanResult performDirectoryScan(Path startPath) {
        AtomicLong localProcessed = new AtomicLong(0);
        AtomicLong localSkipped = new AtomicLong(0);
        
        try {
            // Use parallel stream for directory traversal
            try (Stream<Path> pathStream = Files.walk(startPath)) {
                pathStream
                    .parallel()
                    .filter(Files::isRegularFile)
                    .filter(this::shouldProcessFile)
                    .forEach(filePath -> {
                        if (processFileAsync(filePath)) {
                            localProcessed.incrementAndGet();
                        } else {
                            localSkipped.incrementAndGet();
                        }
                    });
            }
        } catch (IOException e) {
            logger.error("Error scanning directory: {}", startPath, e);
            return new ScanResult(0, 0, "Error: " + e.getMessage());
        }
        
        // Wait for all tasks to complete
        waitForTaskCompletion();
        
        return new ScanResult(localProcessed.get(), localSkipped.get(), null);
    }

    private boolean shouldProcessFile(Path filePath) {
        return fileFilterService.shouldProcess(filePath);
    }

    private boolean processFileAsync(Path filePath) {
        try {
            activeTasks.incrementAndGet();
            
            CompletableFuture
                .supplyAsync(() -> extractFileMetadata(filePath), forkJoinPool)
                .thenApplyAsync(metadata -> generateHashAsync(metadata, filePath), hashingExecutor)
                .thenAccept(this::queueForPersistence)
                .exceptionally(throwable -> {
                    logger.warn("Failed to process file: {}", filePath, throwable);
                    return null;
                })
                .whenComplete((result, throwable) -> activeTasks.decrementAndGet());
                
            return true;
        } catch (Exception e) {
            logger.warn("Unable to process file: {}", filePath, e);
            activeTasks.decrementAndGet();
            return false;
        }
    }

    private FileMetadata extractFileMetadata(Path filePath) {
        try {
            FileMetadata metadata = new FileMetadata();
            metadata.setFileName(filePath.getFileName().toString());
            metadata.setFileDirectory(filePath.getParent().toString());
            metadata.setFileSize(Files.size(filePath));
            metadata.setLastModified(LocalDateTime.ofInstant(
                Files.getLastModifiedTime(filePath).toInstant(), ZoneId.systemDefault()));
            metadata.setContentType(Files.probeContentType(filePath));
            return metadata;
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract metadata for: " + filePath, e);
        }
    }

    private FileMetadata generateHashAsync(FileMetadata metadata, Path filePath) {
        try {
            String hash = HashGenerator.generateHash(filePath);
            metadata.setFileHash(hash);
            return metadata;
        } catch (Exception e) {
            logger.warn("Failed to generate hash for: {}", filePath, e);
            metadata.setFileHash(null);
            return metadata;
        }
    }

    private void queueForPersistence(FileMetadata metadata) {
        try {
            if (!metadataQueue.offer(metadata, 5, TimeUnit.SECONDS)) {
                logger.warn("Failed to queue metadata for: {} - queue is full", metadata.getFileName());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while queueing metadata for: {}", metadata.getFileName());
        }
    }

    private void startMetadataSaver() {
        Thread saverThread = new Thread(() -> {
            List<FileMetadata> batch = new ArrayList<>(properties.getProcessing().getBatchSize());
            
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Wait for at least one item
                    FileMetadata first = metadataQueue.take();
                    batch.add(first);
                    
                    // Drain additional items up to batch size
                    metadataQueue.drainTo(batch, properties.getProcessing().getBatchSize() - 1);
                    
                    if (!batch.isEmpty()) {
                        fileMetadataService.saveFileMetadataBatch(batch);
                        batch.clear();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error saving metadata batch", e);
                }
            }
        }, "metadata-saver");
        
        saverThread.setDaemon(true);
        saverThread.start();
    }

    private void waitForTaskCompletion() {
        while (activeTasks.get() > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public ScanStatistics getStatistics() {
        return new ScanStatistics(
            processedFiles.get(),
            skippedFiles.get(),
            activeTasks.get(),
            metadataQueue.size(),
            isScanning
        );
    }

    @jakarta.annotation.PreDestroy
    public void shutdown() {
        logger.info("Shutting down FileMetadataScanner...");
        
        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            forkJoinPool.shutdown();
        }
        
        if (hashingExecutor != null && !hashingExecutor.isShutdown()) {
            hashingExecutor.shutdown();
        }
    }

    // Data classes for results and statistics
    public static class ScanResult {
        private final long processedFiles;
        private final long skippedFiles;
        private final String error;
        private long durationMs;

        public ScanResult(long processedFiles, long skippedFiles, String error) {
            this.processedFiles = processedFiles;
            this.skippedFiles = skippedFiles;
            this.error = error;
        }

        public ScanResult() {
            this(0, 0, null);
        }

        public static ScanResult combine(ScanResult a, ScanResult b) {
            return new ScanResult(
                a.processedFiles + b.processedFiles,
                a.skippedFiles + b.skippedFiles,
                a.error != null ? a.error : b.error
            );
        }

        // Getters
        public long getProcessedFiles() { return processedFiles; }
        public long getSkippedFiles() { return skippedFiles; }
        public String getError() { return error; }
        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    }

    public static class ScanStatistics {
        private final long totalProcessed;
        private final long totalSkipped;
        private final int activeTasks;
        private final int queueSize;
        private final boolean isScanning;

        public ScanStatistics(long totalProcessed, long totalSkipped, int activeTasks, int queueSize, boolean isScanning) {
            this.totalProcessed = totalProcessed;
            this.totalSkipped = totalSkipped;
            this.activeTasks = activeTasks;
            this.queueSize = queueSize;
            this.isScanning = isScanning;
        }

        // Getters
        public long getTotalProcessed() { return totalProcessed; }
        public long getTotalSkipped() { return totalSkipped; }
        public int getActiveTasks() { return activeTasks; }
        public int getQueueSize() { return queueSize; }
        public boolean isScanning() { return isScanning; }
    }
}
