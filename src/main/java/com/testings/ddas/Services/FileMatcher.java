package com.testings.ddas.Services;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Component
public class FileMatcher {

    // Method to compare two large files bit by bit using NIO with optimization
    public static boolean compareLargeFiles(String filePath1, String filePath2) {
        Path path1 = Paths.get(filePath1);
        Path path2 = Paths.get(filePath2);

        // Ensure both paths exist
        if (!java.nio.file.Files.exists(path1) || !java.nio.file.Files.exists(path2)) {
            System.err.println("One or both file paths do not exist.");
            return false;
        }

        try (FileChannel channel1 = FileChannel.open(path1, StandardOpenOption.READ);
             FileChannel channel2 = FileChannel.open(path2, StandardOpenOption.READ)) {

            // Check if file sizes are the same
            if (channel1.size() != channel2.size()) {
                System.out.println("Files differ in size.");
                return false;
            }

            long fileSize = channel1.size();
            long position = 0;
            long bufferSize = 8 * 1024 * 1024; // 8 MB buffer size

            // Compare files in chunks of bufferSize
            while (position < fileSize) {
                long remaining = fileSize - position;
                long sizeToMap = Math.min(bufferSize, remaining);

                // Map portions of the files into memory
                MappedByteBuffer buffer1 = channel1.map(FileChannel.MapMode.READ_ONLY, position, sizeToMap);
                MappedByteBuffer buffer2 = channel2.map(FileChannel.MapMode.READ_ONLY, position, sizeToMap);

                // Compare the mapped buffers byte by byte
                for (int i = 0; i < sizeToMap; i++) {
                    if (buffer1.get(i) != buffer2.get(i)) {
                        System.out.println("Files differ at byte position: " + (position + i));
                        return false;
                    }
                }

                position += sizeToMap;
            }

            System.out.println("Files are identical.");
            return true;

        } catch (IOException e) {
            System.err.println("Error reading files: " + e.getMessage());
            return false;
        }
    }


}
