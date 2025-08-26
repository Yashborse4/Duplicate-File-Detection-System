package com.testings.ddas.Services;

import com.testings.ddas.Model.FileMetadata;
import com.testings.ddas.Repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;

@Service
public class FileMetadataService {

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Transactional // Ensure this method runs in a transaction
    public void saveFileMetadataBatch(List<FileMetadata> fileMetadataList) {
        if (fileMetadataList.isEmpty()) {
            return; // No data to save
        }

        // Save all metadata in one go
        fileMetadataRepository.saveAll(fileMetadataList);
        System.out.println("Saved batch of " + fileMetadataList.size() + " file metadata records.");
    }

    public FileMetadata saveFileMetadata(FileMetadata fileMetadata) {
        System.out.println("Saving file metadata: " + fileMetadata);
        return fileMetadataRepository.save(fileMetadata);
    }

    public void deleteFileMetadataByPath(Path path, String filename) {
        fileMetadataRepository.deleteByPath(Path.of(path.toUri()), filename);


    }
}
