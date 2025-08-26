package com.testings.ddas.Model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * This class represents the metadata of a file that is stored in the database.
 * It is used to store information about the file, such as its name, directory, size, content type, and hash.
 */
@Entity
@Table(name = "file_metadata")
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_directory", nullable = false)
    private String fileDirectory;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;


    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_hash", length = 65)
    private String fileHash;


    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
    public String getFileDirectory() {
        return fileDirectory;
    }

    public void setFileDirectory(String fileDirectory) {
        this.fileDirectory = fileDirectory;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }


    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }
    // Additional methods if needed, such as getters and setters for additional fields or relationships

    @Override
    public String toString() {
        return "FileMetadata{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", fileDirectory='" + fileDirectory + '\'' +
                ", fileSize=" + fileSize +
                ", contentType='" + contentType + '\'' +
                ", fileHash='" + fileHash + '\'' +
                '}';
    }




}
