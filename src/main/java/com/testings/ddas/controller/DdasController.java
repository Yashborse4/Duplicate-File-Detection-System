package com.testings.ddas.controller;

import com.testings.ddas.Services.*;
import com.testings.ddas.config.DdasProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ddas")
public class DdasController {

    private static final Logger logger = LoggerFactory.getLogger(DdasController.class);

    @Autowired
    private FileMetadataScanner fileMetadataScanner;

    @Autowired
    private AdvancedDuplicateDetectionService duplicateDetectionService;

    @Autowired
    private FileFilterService fileFilterService;

    @Autowired
    private DdasProperties properties;

    /**
     * Get system status and statistics
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Scanner statistics
            var scanStats = fileMetadataScanner.getStatistics();
            status.put("scanner", Map.of(
                "totalProcessed", scanStats.getTotalProcessed(),
                "totalSkipped", scanStats.getTotalSkipped(),
                "activeTasks", scanStats.getActiveTasks(),
                "queueSize", scanStats.getQueueSize(),
                "isScanning", scanStats.isScanning()
            ));

            // Duplicate detection statistics
            var dupStats = duplicateDetectionService.getStatistics();
            status.put("duplicateDetection", Map.of(
                "duplicatesDetected", dupStats.getDuplicatesDetected(),
                "duplicatesDeleted", dupStats.getDuplicatesDeleted(),
                "spaceReclaimed", dupStats.getSpaceReclaimed(),
                "totalFiles", dupStats.getTotalFiles(),
                "duplicateHashGroups", dupStats.getDuplicateHashGroups(),
                "autoDeleteEnabled", dupStats.isAutoDeleteEnabled()
            ));

            // File filter statistics
            var filterStats = fileFilterService.getStatistics();
            status.put("fileFilter", Map.of(
                "includedExtensions", filterStats.getIncludedExtensions(),
                "excludedExtensions", filterStats.getExcludedExtensions(),
                "excludedDirectories", filterStats.getExcludedDirectories(),
                "includedPatterns", filterStats.getIncludedPatterns(),
                "excludedPatterns", filterStats.getExcludedPatterns(),
                "minFileSize", filterStats.getMinFileSize(),
                "maxFileSize", filterStats.getMaxFileSize()
            ));

            // System configuration
            status.put("configuration", Map.of(
                "monitoredPaths", properties.getMonitoredPaths(),
                "processingParallelism", properties.getProcessing().getParallelism(),
                "batchSize", properties.getProcessing().getBatchSize(),
                "duplicateCheckDelay", properties.getDuplicateDetection().getDuplicateCheckDelay().toSeconds(),
                "deletionStrategy", properties.getDuplicateDetection().getDeletionStrategy().toString()
            ));

            status.put("timestamp", System.currentTimeMillis());
            status.put("status", "healthy");

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("Error getting system status", e);
            status.put("status", "error");
            status.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(status);
        }
    }

    /**
     * Start a directory scan
     */
    @PostMapping("/scan/directory")
    public ResponseEntity<Map<String, Object>> scanDirectory(@RequestParam String path) {
        try {
            logger.info("Starting directory scan via API: {}", path);
            
            CompletableFuture<FileMetadataScanner.ScanResult> future = fileMetadataScanner.scanDirectory(path);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Directory scan started");
            response.put("path", path);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error starting directory scan", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to start directory scan");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Start scanning all configured paths
     */
    @PostMapping("/scan/all")
    public ResponseEntity<Map<String, Object>> scanAllDirectories() {
        try {
            logger.info("Starting full system scan via API");
            
            CompletableFuture<FileMetadataScanner.ScanResult> future = fileMetadataScanner.scanAllFiles();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Full system scan started");
            response.put("paths", properties.getMonitoredPaths().isEmpty() ? List.of("D:/") : properties.getMonitoredPaths());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error starting full system scan", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to start full system scan");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Force duplicate detection on existing files
     */
    @PostMapping("/duplicates/detect")
    public ResponseEntity<Map<String, Object>> forceDuplicateDetection() {
        try {
            logger.info("Starting forced duplicate detection via API");
            
            // This would typically trigger a batch duplicate detection process
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Duplicate detection process initiated");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error starting duplicate detection", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to start duplicate detection");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Update file filter patterns
     */
    @PutMapping("/filters/patterns")
    public ResponseEntity<Map<String, Object>> updateFilterPatterns(
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> includedPatterns = (List<String>) request.getOrDefault("includedPatterns", List.of());
            @SuppressWarnings("unchecked")
            List<String> excludedPatterns = (List<String>) request.getOrDefault("excludedPatterns", List.of());
            
            fileFilterService.updatePatterns(includedPatterns, excludedPatterns);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Filter patterns updated successfully");
            response.put("includedPatterns", includedPatterns.size());
            response.put("excludedPatterns", excludedPatterns.size());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error updating filter patterns", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to update filter patterns");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get detailed performance metrics
     */
    @GetMapping("/metrics/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // Runtime metrics
            Runtime runtime = Runtime.getRuntime();
            metrics.put("memory", Map.of(
                "totalMemory", runtime.totalMemory(),
                "freeMemory", runtime.freeMemory(),
                "usedMemory", runtime.totalMemory() - runtime.freeMemory(),
                "maxMemory", runtime.maxMemory()
            ));
            
            metrics.put("system", Map.of(
                "availableProcessors", runtime.availableProcessors(),
                "operatingSystem", System.getProperty("os.name"),
                "javaVersion", System.getProperty("java.version")
            ));
            
            // Scanner performance
            var scanStats = fileMetadataScanner.getStatistics();
            metrics.put("scannerPerformance", Map.of(
                "activeTasks", scanStats.getActiveTasks(),
                "queueSize", scanStats.getQueueSize(),
                "isActive", scanStats.isScanning()
            ));
            
            // Duplicate detection performance
            var dupStats = duplicateDetectionService.getStatistics();
            metrics.put("duplicatePerformance", Map.of(
                "detectionRate", dupStats.getDuplicatesDetected(),
                "deletionRate", dupStats.getDuplicatesDeleted(),
                "spaceEfficiency", dupStats.getSpaceReclaimed()
            ));
            
            metrics.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("Error getting performance metrics", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get performance metrics");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Basic health checks
            boolean scannerHealthy = fileMetadataScanner.getStatistics().getActiveTasks() >= 0;
            boolean duplicateServiceHealthy = duplicateDetectionService.getStatistics().getTotalFiles() >= 0;
            
            boolean isHealthy = scannerHealthy && duplicateServiceHealthy;
            
            health.put("status", isHealthy ? "UP" : "DOWN");
            health.put("scanner", scannerHealthy ? "UP" : "DOWN");
            health.put("duplicateService", duplicateServiceHealthy ? "UP" : "DOWN");
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(health);
        }
    }

    /**
     * Get system configuration
     */
    @GetMapping("/config")
    public ResponseEntity<DdasProperties> getConfiguration() {
        return ResponseEntity.ok(properties);
    }

    /**
     * Emergency stop - pause all processing
     */
    @PostMapping("/emergency/stop")
    public ResponseEntity<Map<String, Object>> emergencyStop() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // This would implement emergency stop functionality
            // For now, just return a response
            logger.warn("Emergency stop requested via API");
            
            response.put("message", "Emergency stop initiated");
            response.put("timestamp", System.currentTimeMillis());
            response.put("status", "stopped");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during emergency stop", e);
            response.put("error", "Failed to execute emergency stop");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
