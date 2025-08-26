package com.testings.ddas.Services;

import com.testings.ddas.config.DdasProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FileFilterService {

    private static final Logger logger = LoggerFactory.getLogger(FileFilterService.class);

    @Autowired
    private DdasProperties properties;

    private final Set<Pattern> includedPatterns = ConcurrentHashMap.newKeySet();
    private final Set<Pattern> excludedPatterns = ConcurrentHashMap.newKeySet();
    private volatile boolean patternsInitialized = false;

    @jakarta.annotation.PostConstruct
    private void initializePatterns() {
        var fileFilter = properties.getFileFilter();
        
        // Compile included patterns
        fileFilter.getIncludedPatterns().forEach(pattern -> {
            try {
                includedPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            } catch (Exception e) {
                logger.warn("Invalid included pattern: {}", pattern, e);
            }
        });

        // Compile excluded patterns
        fileFilter.getExcludedPatterns().forEach(pattern -> {
            try {
                excludedPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            } catch (Exception e) {
                logger.warn("Invalid excluded pattern: {}", pattern, e);
            }
        });

        patternsInitialized = true;
        logger.info("Initialized file filters - {} included patterns, {} excluded patterns", 
                   includedPatterns.size(), excludedPatterns.size());
    }

    /**
     * Determines if a file should be processed based on configured filters
     */
    public boolean shouldProcess(Path filePath) {
        try {
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return false;
            }

            var fileFilter = properties.getFileFilter();
            String fileName = filePath.getFileName().toString();
            String extension = getFileExtension(fileName);
            
            // Check if file is hidden and should be skipped
            if (fileFilter.isSkipHiddenFiles() && Files.isHidden(filePath)) {
                return false;
            }

            // Check if file is system file and should be skipped
            if (fileFilter.isSkipSystemFiles() && isSystemFile(filePath)) {
                return false;
            }

            // Check if file is read-only and should be skipped
            if (fileFilter.isSkipReadOnlyFiles() && !Files.isWritable(filePath)) {
                return false;
            }

            // Check directory exclusions
            if (isInExcludedDirectory(filePath, fileFilter.getExcludedDirectories())) {
                return false;
            }

            // Check file size constraints
            long fileSize = Files.size(filePath);
            if (fileSize < fileFilter.getMinFileSize() || fileSize > fileFilter.getMaxFileSize()) {
                return false;
            }

            // Check extension filters
            if (!isExtensionAllowed(extension, fileFilter.getIncludedExtensions(), fileFilter.getExcludedExtensions())) {
                return false;
            }

            // Check pattern filters
            if (!isPatternAllowed(fileName)) {
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.warn("Error checking if file should be processed: {}", filePath, e);
            return false;
        }
    }

    /**
     * Checks if a file is in an excluded directory
     */
    private boolean isInExcludedDirectory(Path filePath, List<String> excludedDirectories) {
        String pathString = filePath.toString();
        return excludedDirectories.stream()
            .anyMatch(excluded -> pathString.contains(excluded));
    }

    /**
     * Checks if file extension is allowed based on include/exclude lists
     */
    private boolean isExtensionAllowed(String extension, List<String> included, List<String> excluded) {
        // If included extensions are specified, file must match one of them
        if (!included.isEmpty()) {
            boolean matchesIncluded = included.stream()
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));
            if (!matchesIncluded) {
                return false;
            }
        }

        // Check if extension is explicitly excluded
        return excluded.stream()
            .noneMatch(ext -> ext.equalsIgnoreCase(extension));
    }

    /**
     * Checks if filename matches pattern filters
     */
    private boolean isPatternAllowed(String fileName) {
        if (!patternsInitialized) {
            return true; // Default to allow if patterns not initialized yet
        }

        // If included patterns exist, filename must match at least one
        if (!includedPatterns.isEmpty()) {
            boolean matchesIncluded = includedPatterns.stream()
                .anyMatch(pattern -> pattern.matcher(fileName).matches());
            if (!matchesIncluded) {
                return false;
            }
        }

        // Check if filename matches any excluded pattern
        return excludedPatterns.stream()
            .noneMatch(pattern -> pattern.matcher(fileName).matches());
    }

    /**
     * Extracts file extension from filename
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }

    /**
     * Determines if a file is a system file
     */
    private boolean isSystemFile(Path filePath) {
        try {
            // Check for common system file attributes on Windows
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                return Files.getAttribute(filePath, "dos:system", java.nio.file.LinkOption.NOFOLLOW_LINKS).equals(true);
            }
            
            // For Unix-like systems, check if file starts with dot (hidden) or is in system directories
            String fileName = filePath.getFileName().toString();
            return fileName.startsWith(".") || 
                   filePath.toString().contains("/proc/") ||
                   filePath.toString().contains("/sys/") ||
                   filePath.toString().contains("/dev/");
                   
        } catch (Exception e) {
            // If we can't determine, assume it's not a system file
            return false;
        }
    }

    /**
     * Gets file filtering statistics
     */
    public FilterStatistics getStatistics() {
        var fileFilter = properties.getFileFilter();
        return new FilterStatistics(
            fileFilter.getIncludedExtensions().size(),
            fileFilter.getExcludedExtensions().size(),
            fileFilter.getExcludedDirectories().size(),
            includedPatterns.size(),
            excludedPatterns.size(),
            fileFilter.getMinFileSize(),
            fileFilter.getMaxFileSize()
        );
    }

    /**
     * Updates filter patterns at runtime
     */
    public void updatePatterns(List<String> newIncludedPatterns, List<String> newExcludedPatterns) {
        includedPatterns.clear();
        excludedPatterns.clear();

        newIncludedPatterns.forEach(pattern -> {
            try {
                includedPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            } catch (Exception e) {
                logger.warn("Invalid included pattern during update: {}", pattern, e);
            }
        });

        newExcludedPatterns.forEach(pattern -> {
            try {
                excludedPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            } catch (Exception e) {
                logger.warn("Invalid excluded pattern during update: {}", pattern, e);
            }
        });

        logger.info("Updated file filter patterns - {} included, {} excluded", 
                   includedPatterns.size(), excludedPatterns.size());
    }

    public static class FilterStatistics {
        private final int includedExtensions;
        private final int excludedExtensions;
        private final int excludedDirectories;
        private final int includedPatterns;
        private final int excludedPatterns;
        private final long minFileSize;
        private final long maxFileSize;

        public FilterStatistics(int includedExtensions, int excludedExtensions, int excludedDirectories,
                              int includedPatterns, int excludedPatterns, long minFileSize, long maxFileSize) {
            this.includedExtensions = includedExtensions;
            this.excludedExtensions = excludedExtensions;
            this.excludedDirectories = excludedDirectories;
            this.includedPatterns = includedPatterns;
            this.excludedPatterns = excludedPatterns;
            this.minFileSize = minFileSize;
            this.maxFileSize = maxFileSize;
        }

        // Getters
        public int getIncludedExtensions() { return includedExtensions; }
        public int getExcludedExtensions() { return excludedExtensions; }
        public int getExcludedDirectories() { return excludedDirectories; }
        public int getIncludedPatterns() { return includedPatterns; }
        public int getExcludedPatterns() { return excludedPatterns; }
        public long getMinFileSize() { return minFileSize; }
        public long getMaxFileSize() { return maxFileSize; }
    }
}
