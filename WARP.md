# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

DDAS is a **Duplicate Detection and Storage Optimization System** - a Spring Boot application that automatically detects and removes duplicate files from the file system to optimize storage usage. The system monitors file changes in real-time, generates SHA-256 hashes for content verification, and automatically deletes duplicate files when hash matches are found. This is particularly useful for environments with large downloads where storage optimization is critical.

## Build and Development Commands

### Build and Run
```powershell
# Build the project
.\gradlew build

# Run the application with optimized JVM settings
.\gradlew bootRun --args="--spring.profiles.active=production"

# Run with custom JVM options for better performance
java -Xms2G -Xmx8G -XX:+UseG1GC -XX:+UseStringDeduplication -jar build/libs/DDAS-0.0.1-SNAPSHOT.jar

# Run tests
.\gradlew test

# Clean build artifacts
.\gradlew clean
```

### Performance Testing
```powershell
# Run with performance monitoring
.\gradlew bootRun --args="--ddas.monitoring.enabled=true --ddas.performance.enable-caching=true"

# Run with debug logging for performance analysis
.\gradlew bootRun --args="--logging.level.com.testings.ddas=DEBUG"
```

### Database Setup
The application requires MySQL database configuration:
- Database: `DDASDB`
- Connection details in `application.properties`
- Ensure MySQL is running before starting the application

### Testing
```powershell
# Run all tests
.\gradlew test

# Run specific test class
.\gradlew test --tests DdasApplicationTests

# Run with detailed output
.\gradlew test --info
```

## Architecture Overview

### Core Components

**FileMetadata Model**: Central entity storing file information (name, directory, size, hash, content type, last modified). Uses JPA with MySQL persistence.

**DirectoryWatcher**: Real-time file system monitoring service that watches the entire D:/ drive by default. Automatically registers new directories and processes file creation/deletion events. Implements intelligent filtering to skip system directories like `$RECYCLE.BIN`.

**FileMetadataScanner**: Bulk file processing service using multi-threading with executor pools. Processes files in batches of 200 for efficient database operations. Designed for initial system scans or periodic full-system analysis.

**HashGenerator**: SHA-256 hash generation utility for file content verification and duplicate detection. Uses Base64 encoding for hash storage.

**FileMatcher**: Binary file comparison service using memory-mapped I/O with 8MB buffer chunks. Optimized for comparing large files efficiently.

### Data Flow Architecture

1. **Initial Scan**: `FileMetadataScanner` performs bulk directory traversal with concurrent processing
2. **Real-time Monitoring**: `DirectoryWatcher` continuously monitors for file system changes
3. **Metadata Extraction**: Both services extract file metadata (size, hash, content type, timestamps)
4. **Hash Generation**: SHA-256 hashes are computed for file content verification
5. **Database Persistence**: `FileMetadataService` handles single file and batch operations with transaction management
6. **Duplicate Detection**: Hash-based comparison identifies duplicate files in the database
7. **Automatic Deletion**: When duplicate hashes are found, the system automatically deletes redundant files to optimize storage

### Key Design Patterns

- **Service Layer Architecture**: Clear separation between controllers, services, and repositories
- **Thread Pool Management**: Concurrent processing using fixed thread pools based on available processors
- **Batch Processing**: Queue-based metadata collection with configurable batch sizes
- **File System Events**: Java NIO WatchService for efficient directory monitoring
- **Transaction Management**: JPA transactions for data consistency

### Performance Considerations

- **Memory Mapping**: Large file operations use memory-mapped buffers
- **System Directory Filtering**: Automatic exclusion of Windows system directories
- **Concurrent Processing**: Multi-threaded file processing with shared thread pools
- **Batch Database Operations**: Reduces database round-trips through batch inserts

### Database Schema

The `file_metadata` table stores:
- Unique ID (auto-generated)
- File name and directory path
- File size and content type
- SHA-256 hash for duplicate detection
- Last modified timestamp

### Configuration Notes

- **Monitored Path**: Currently hardcoded to D:/ drive (configurable in `DdasApplication.java`)
- **Database**: MySQL with JPA/Hibernate ORM
- **Thread Pool Size**: Automatically set to available processor count
- **Batch Size**: 200 files per database batch operation

## Development Environment

- **Java Version**: 21 (configured in build.gradle)
- **Spring Boot**: 3.3.3
- **Database**: MySQL (with PostgreSQL and Oracle drivers available)
- **Build Tool**: Gradle with Wrapper included
- **Testing**: JUnit 5 Platform

## Storage Optimization Features

### Duplicate Detection Strategy
- **Hash-Based Comparison**: Uses SHA-256 hashes to identify identical file content
- **Size Verification**: Pre-filters files by size before hash comparison for efficiency
- **Automatic Cleanup**: Deletes duplicate files automatically to free up storage space
- **Large File Support**: Optimized for environments with frequent large downloads

### Safety Mechanisms
- **System Directory Protection**: Automatically excludes critical system directories
- **Transaction Safety**: Database operations are wrapped in transactions
- **Error Handling**: Graceful handling of file access permissions and I/O errors

## Common Operations

### Adding Duplicate Detection Logic
1. Implement hash comparison queries in `FileMetadataRepository`
2. Add duplicate detection methods in `FileMetadataService`
3. Update file processing logic to check for existing hashes before saving
4. Implement file deletion logic when duplicates are found

### Adding New File Processing Logic
1. Modify `processFile()` method in `FileMetadataScanner` or `DirectoryWatcher`
2. Update `FileMetadata` entity if new fields needed
3. Ensure database schema matches entity changes
4. Add corresponding repository methods if custom queries required

### Changing Monitored Directories
Update the hardcoded path in `DdasApplication.run()` method - currently set to `Paths.get("D:/")`.

### Implementing Custom Duplicate Rules
1. Create custom comparison logic beyond hash matching (e.g., filename patterns)
2. Add configuration for which files to keep vs. delete when duplicates found
3. Implement user preferences for duplicate handling strategies

### Database Migration
- Hibernate DDL is set to "none" - manual schema management required
- Update `spring.jpa.hibernate.ddl-auto` in application.properties for automatic schema updates during development

## Advanced Optimization Features

### Parallel Processing Enhancements
- **ForkJoinPool**: Uses work-stealing algorithm for optimal CPU utilization
- **Virtual Threads**: Supports Java 21 virtual threads for improved concurrency
- **Parallel Streams**: Directory traversal uses parallel processing
- **Async Operations**: File hashing and metadata extraction run asynchronously
- **Configurable Parallelism**: Thread pool sizes based on CPU cores and workload

### Memory and I/O Optimizations
- **Memory-Mapped Files**: Large file operations use memory mapping for efficiency
- **Batch Processing**: Configurable batch sizes for database operations (default: 500)
- **Connection Pooling**: HikariCP with optimized pool settings
- **Buffer Management**: 8MB I/O buffers for file operations
- **Caching Layer**: In-memory caching for frequently accessed file metadata

### Database Performance
- **Batch Inserts**: Hibernate batch processing for multiple file records
- **Query Optimization**: Indexed queries on file_hash column for duplicate detection
- **Connection Pool**: 20 concurrent connections with idle timeout management
- **Transaction Batching**: Groups database operations for better throughput

### Configuration Tuning
```properties
# High-performance configuration examples
ddas.processing.parallelism=16
ddas.processing.batch-size=1000
ddas.performance.io-buffer-size=16777216
ddas.database.batch-size=2000
spring.datasource.hikari.maximum-pool-size=30
```

### Monitoring and Metrics
- **REST API**: Full system monitoring via `/api/ddas/status` endpoint
- **JMX Metrics**: Performance metrics exposed via JMX
- **Health Checks**: System health monitoring at `/api/ddas/health`
- **Real-time Statistics**: Active task counts, queue sizes, processing rates

### Advanced File Filtering
- **Pattern-Based**: Regex patterns for complex file filtering
- **Size-Based**: Configurable min/max file size limits
- **Extension Filtering**: Include/exclude file types
- **Directory Exclusion**: Skip system and temporary directories
- **Dynamic Updates**: Runtime filter pattern updates via API

### Duplicate Detection Strategies
- **Multiple Policies**: KEEP_OLDEST, KEEP_NEWEST, KEEP_SMALLEST, KEEP_LARGEST
- **Protected Files**: Configurable file extensions to never delete
- **Size Thresholds**: Only check duplicates above specified file size
- **Scheduled Detection**: Automatic periodic duplicate scans
- **Batch Processing**: Efficient bulk duplicate detection

## REST API Endpoints

```http
GET /api/ddas/status           # System status and statistics
GET /api/ddas/health           # Health check
GET /api/ddas/metrics/performance  # Detailed performance metrics
POST /api/ddas/scan/directory  # Start directory scan
POST /api/ddas/scan/all        # Scan all monitored paths
PUT /api/ddas/filters/patterns # Update file filter patterns
```

### Performance Tuning
- Adjust `ddas.processing.batch-size` based on memory constraints
- Modify `ddas.processing.parallelism` for specific hardware configurations
- Configure `ddas.database.max-pool-size` for database throughput
- Index the `file_hash` column for faster duplicate lookups
- Use SSD storage for better I/O performance
- Allocate sufficient heap memory (recommended: 4-8GB for large datasets)
