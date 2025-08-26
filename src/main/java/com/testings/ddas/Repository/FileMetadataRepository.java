package com.testings.ddas.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.testings.ddas.Model.FileMetadata;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM FileMetadata f WHERE f.fileDirectory = :path AND f.fileName = :filename")
    void deleteByPath(@Param("path") Path path, @Param("filename") String filename);

    /**
     * Find files with the same hash excluding a specific file ID
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.fileHash = :hash AND f.id != :excludeId")
    List<FileMetadata> findByFileHashAndIdNot(@Param("hash") String hash, @Param("excludeId") Long excludeId);

    /**
     * Find files with the same hash
     */
    List<FileMetadata> findByFileHash(String fileHash);

    /**
     * Find all file hashes that have duplicates (more than one file with same hash)
     */
    @Query("SELECT f.fileHash FROM FileMetadata f WHERE f.fileHash IS NOT NULL AND f.fileSize >= :minSize GROUP BY f.fileHash HAVING COUNT(f) > 1")
    List<String> findDuplicateHashes(@Param("minSize") long minSize);

    /**
     * Count the number of hash groups that have duplicates
     */
    @Query("SELECT COUNT(DISTINCT f.fileHash) FROM FileMetadata f WHERE f.fileHash IS NOT NULL AND f.fileSize >= :minSize AND f.fileHash IN (SELECT f2.fileHash FROM FileMetadata f2 WHERE f2.fileHash IS NOT NULL GROUP BY f2.fileHash HAVING COUNT(f2) > 1)")
    long countDuplicateHashes(@Param("minSize") long minSize);

    /**
     * Find files by size range
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.fileSize BETWEEN :minSize AND :maxSize")
    List<FileMetadata> findByFileSizeBetween(@Param("minSize") long minSize, @Param("maxSize") long maxSize);

    /**
     * Find files by directory pattern
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.fileDirectory LIKE %:pattern%")
    List<FileMetadata> findByDirectoryContaining(@Param("pattern") String pattern);

    /**
     * Count files by content type
     */
    @Query("SELECT f.contentType, COUNT(f) FROM FileMetadata f WHERE f.contentType IS NOT NULL GROUP BY f.contentType")
    List<Object[]> countByContentType();

    /**
     * Get total storage used
     */
    @Query("SELECT SUM(f.fileSize) FROM FileMetadata f")
    Long getTotalStorageUsed();

    /**
     * Get potential storage savings from duplicates
     */
    @Query("SELECT SUM(f.fileSize * (sub.cnt - 1)) FROM FileMetadata f JOIN (SELECT f2.fileHash, COUNT(f2) as cnt FROM FileMetadata f2 WHERE f2.fileHash IS NOT NULL AND f2.fileSize >= :minSize GROUP BY f2.fileHash HAVING COUNT(f2) > 1) sub ON f.fileHash = sub.fileHash WHERE f.fileSize >= :minSize")
    Long getPotentialStorageSavings(@Param("minSize") long minSize);

}
