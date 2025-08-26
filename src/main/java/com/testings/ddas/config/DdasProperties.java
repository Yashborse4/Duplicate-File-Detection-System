package com.testings.ddas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "ddas")
public class DdasProperties {

    private Monitoring monitoring = new Monitoring();
    private Processing processing = new Processing();
    private DuplicateDetection duplicateDetection = new DuplicateDetection();
    private FileFilter fileFilter = new FileFilter();
    private Database database = new Database();
    private Performance performance = new Performance();
    private List<String> monitoredPaths = new ArrayList<>();

    public static class Monitoring {
        private boolean enabled = true;
        private Duration reportInterval = Duration.ofMinutes(5);
        private boolean enableHealthChecks = true;
        private boolean enableJmxMetrics = true;
        private String metricsEndpoint = "/actuator/metrics";

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Duration getReportInterval() { return reportInterval; }
        public void setReportInterval(Duration reportInterval) { this.reportInterval = reportInterval; }
        public boolean isEnableHealthChecks() { return enableHealthChecks; }
        public void setEnableHealthChecks(boolean enableHealthChecks) { this.enableHealthChecks = enableHealthChecks; }
        public boolean isEnableJmxMetrics() { return enableJmxMetrics; }
        public void setEnableJmxMetrics(boolean enableJmxMetrics) { this.enableJmxMetrics = enableJmxMetrics; }
        public String getMetricsEndpoint() { return metricsEndpoint; }
        public void setMetricsEndpoint(String metricsEndpoint) { this.metricsEndpoint = metricsEndpoint; }
    }

    public static class Processing {
        private int corePoolSize = Runtime.getRuntime().availableProcessors();
        private int maxPoolSize = Runtime.getRuntime().availableProcessors() * 2;
        private int queueCapacity = 10000;
        private Duration keepAliveTime = Duration.ofMinutes(1);
        private int batchSize = 500;
        private int parallelism = Runtime.getRuntime().availableProcessors();
        private boolean useWorkStealing = true;
        private boolean enableVirtualThreads = true;
        private int maxConcurrentScans = 3;

        // Getters and setters
        public int getCorePoolSize() { return corePoolSize; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
        public Duration getKeepAliveTime() { return keepAliveTime; }
        public void setKeepAliveTime(Duration keepAliveTime) { this.keepAliveTime = keepAliveTime; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public int getParallelism() { return parallelism; }
        public void setParallelism(int parallelism) { this.parallelism = parallelism; }
        public boolean isUseWorkStealing() { return useWorkStealing; }
        public void setUseWorkStealing(boolean useWorkStealing) { this.useWorkStealing = useWorkStealing; }
        public boolean isEnableVirtualThreads() { return enableVirtualThreads; }
        public void setEnableVirtualThreads(boolean enableVirtualThreads) { this.enableVirtualThreads = enableVirtualThreads; }
        public int getMaxConcurrentScans() { return maxConcurrentScans; }
        public void setMaxConcurrentScans(int maxConcurrentScans) { this.maxConcurrentScans = maxConcurrentScans; }
    }

    public static class DuplicateDetection {
        private boolean autoDelete = true;
        private DeletionStrategy deletionStrategy = DeletionStrategy.KEEP_OLDEST;
        private boolean requireExactMatch = true;
        private long minFileSizeForDuplication = 1024; // 1KB
        private boolean enableSimilarityDetection = false;
        private double similarityThreshold = 0.95;
        private Duration duplicateCheckDelay = Duration.ofSeconds(30);
        private boolean preserveFileStructure = true;
        private Set<String> protectedExtensions = Set.of(".exe", ".dll", ".sys", ".ini");
        private boolean enableDeduplicationLogging = true;

        public enum DeletionStrategy {
            KEEP_OLDEST, KEEP_NEWEST, KEEP_SMALLEST, KEEP_LARGEST, KEEP_FIRST_FOUND
        }

        // Getters and setters
        public boolean isAutoDelete() { return autoDelete; }
        public void setAutoDelete(boolean autoDelete) { this.autoDelete = autoDelete; }
        public DeletionStrategy getDeletionStrategy() { return deletionStrategy; }
        public void setDeletionStrategy(DeletionStrategy deletionStrategy) { this.deletionStrategy = deletionStrategy; }
        public boolean isRequireExactMatch() { return requireExactMatch; }
        public void setRequireExactMatch(boolean requireExactMatch) { this.requireExactMatch = requireExactMatch; }
        public long getMinFileSizeForDuplication() { return minFileSizeForDuplication; }
        public void setMinFileSizeForDuplication(long minFileSizeForDuplication) { this.minFileSizeForDuplication = minFileSizeForDuplication; }
        public boolean isEnableSimilarityDetection() { return enableSimilarityDetection; }
        public void setEnableSimilarityDetection(boolean enableSimilarityDetection) { this.enableSimilarityDetection = enableSimilarityDetection; }
        public double getSimilarityThreshold() { return similarityThreshold; }
        public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
        public Duration getDuplicateCheckDelay() { return duplicateCheckDelay; }
        public void setDuplicateCheckDelay(Duration duplicateCheckDelay) { this.duplicateCheckDelay = duplicateCheckDelay; }
        public boolean isPreserveFileStructure() { return preserveFileStructure; }
        public void setPreserveFileStructure(boolean preserveFileStructure) { this.preserveFileStructure = preserveFileStructure; }
        public Set<String> getProtectedExtensions() { return protectedExtensions; }
        public void setProtectedExtensions(Set<String> protectedExtensions) { this.protectedExtensions = protectedExtensions; }
        public boolean isEnableDeduplicationLogging() { return enableDeduplicationLogging; }
        public void setEnableDeduplicationLogging(boolean enableDeduplicationLogging) { this.enableDeduplicationLogging = enableDeduplicationLogging; }
    }

    public static class FileFilter {
        private List<String> includedExtensions = new ArrayList<>();
        private List<String> excludedExtensions = List.of(".tmp", ".log", ".cache", ".lock");
        private List<String> excludedDirectories = List.of("$RECYCLE.BIN", "System Volume Information", ".git", "node_modules");
        private long maxFileSize = Long.MAX_VALUE;
        private long minFileSize = 0;
        private List<String> includedPatterns = new ArrayList<>();
        private List<String> excludedPatterns = new ArrayList<>();
        private boolean skipHiddenFiles = true;
        private boolean skipSystemFiles = true;
        private boolean skipReadOnlyFiles = false;

        // Getters and setters
        public List<String> getIncludedExtensions() { return includedExtensions; }
        public void setIncludedExtensions(List<String> includedExtensions) { this.includedExtensions = includedExtensions; }
        public List<String> getExcludedExtensions() { return excludedExtensions; }
        public void setExcludedExtensions(List<String> excludedExtensions) { this.excludedExtensions = excludedExtensions; }
        public List<String> getExcludedDirectories() { return excludedDirectories; }
        public void setExcludedDirectories(List<String> excludedDirectories) { this.excludedDirectories = excludedDirectories; }
        public long getMaxFileSize() { return maxFileSize; }
        public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }
        public long getMinFileSize() { return minFileSize; }
        public void setMinFileSize(long minFileSize) { this.minFileSize = minFileSize; }
        public List<String> getIncludedPatterns() { return includedPatterns; }
        public void setIncludedPatterns(List<String> includedPatterns) { this.includedPatterns = includedPatterns; }
        public List<String> getExcludedPatterns() { return excludedPatterns; }
        public void setExcludedPatterns(List<String> excludedPatterns) { this.excludedPatterns = excludedPatterns; }
        public boolean isSkipHiddenFiles() { return skipHiddenFiles; }
        public void setSkipHiddenFiles(boolean skipHiddenFiles) { this.skipHiddenFiles = skipHiddenFiles; }
        public boolean isSkipSystemFiles() { return skipSystemFiles; }
        public void setSkipSystemFiles(boolean skipSystemFiles) { this.skipSystemFiles = skipSystemFiles; }
        public boolean isSkipReadOnlyFiles() { return skipReadOnlyFiles; }
        public void setSkipReadOnlyFiles(boolean skipReadOnlyFiles) { this.skipReadOnlyFiles = skipReadOnlyFiles; }
    }

    public static class Database {
        private int batchSize = 1000;
        private int maxPoolSize = 20;
        private Duration connectionTimeout = Duration.ofSeconds(30);
        private boolean enableQueryLogging = false;
        private boolean enableStatistics = true;
        private int fetchSize = 1000;

        // Getters and setters
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public Duration getConnectionTimeout() { return connectionTimeout; }
        public void setConnectionTimeout(Duration connectionTimeout) { this.connectionTimeout = connectionTimeout; }
        public boolean isEnableQueryLogging() { return enableQueryLogging; }
        public void setEnableQueryLogging(boolean enableQueryLogging) { this.enableQueryLogging = enableQueryLogging; }
        public boolean isEnableStatistics() { return enableStatistics; }
        public void setEnableStatistics(boolean enableStatistics) { this.enableStatistics = enableStatistics; }
        public int getFetchSize() { return fetchSize; }
        public void setFetchSize(int fetchSize) { this.fetchSize = fetchSize; }
    }

    public static class Performance {
        private int ioBufferSize = 8 * 1024 * 1024; // 8MB
        private boolean useMemoryMapping = true;
        private int hashingThreads = Runtime.getRuntime().availableProcessors();
        private boolean enableCaching = true;
        private int cacheSize = 10000;
        private Duration cacheExpiration = Duration.ofHours(1);
        private boolean useNativeHashing = true;

        // Getters and setters
        public int getIoBufferSize() { return ioBufferSize; }
        public void setIoBufferSize(int ioBufferSize) { this.ioBufferSize = ioBufferSize; }
        public boolean isUseMemoryMapping() { return useMemoryMapping; }
        public void setUseMemoryMapping(boolean useMemoryMapping) { this.useMemoryMapping = useMemoryMapping; }
        public int getHashingThreads() { return hashingThreads; }
        public void setHashingThreads(int hashingThreads) { this.hashingThreads = hashingThreads; }
        public boolean isEnableCaching() { return enableCaching; }
        public void setEnableCaching(boolean enableCaching) { this.enableCaching = enableCaching; }
        public int getCacheSize() { return cacheSize; }
        public void setCacheSize(int cacheSize) { this.cacheSize = cacheSize; }
        public Duration getCacheExpiration() { return cacheExpiration; }
        public void setCacheExpiration(Duration cacheExpiration) { this.cacheExpiration = cacheExpiration; }
        public boolean isUseNativeHashing() { return useNativeHashing; }
        public void setUseNativeHashing(boolean useNativeHashing) { this.useNativeHashing = useNativeHashing; }
    }

    // Main class getters and setters
    public Monitoring getMonitoring() { return monitoring; }
    public void setMonitoring(Monitoring monitoring) { this.monitoring = monitoring; }
    public Processing getProcessing() { return processing; }
    public void setProcessing(Processing processing) { this.processing = processing; }
    public DuplicateDetection getDuplicateDetection() { return duplicateDetection; }
    public void setDuplicateDetection(DuplicateDetection duplicateDetection) { this.duplicateDetection = duplicateDetection; }
    public FileFilter getFileFilter() { return fileFilter; }
    public void setFileFilter(FileFilter fileFilter) { this.fileFilter = fileFilter; }
    public Database getDatabase() { return database; }
    public void setDatabase(Database database) { this.database = database; }
    public Performance getPerformance() { return performance; }
    public void setPerformance(Performance performance) { this.performance = performance; }
    public List<String> getMonitoredPaths() { return monitoredPaths; }
    public void setMonitoredPaths(List<String> monitoredPaths) { this.monitoredPaths = monitoredPaths; }
}
