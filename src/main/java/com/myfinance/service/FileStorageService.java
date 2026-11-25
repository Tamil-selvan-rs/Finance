package com.myfinance.service;

import com.myfinance.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload.dir:uploads/receipts}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize secure upload directory.", e);
        }
    }

    public String storeOptionalFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "receipt.png";
        String ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        String safeFileName = UUID.randomUUID() + ext;

        try {
            Path target = this.uploadRoot.resolve(safeFileName).normalize();
            if (!target.startsWith(this.uploadRoot)) {
                throw new FileStorageException("Path traversal attempt detected.");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return safeFileName;
        } catch (IOException e) {
            throw new FileStorageException("Failed to store uploaded receipt image.", e);
        }
    }

    public Path resolveFilePath(String storedFileName) {
        return this.uploadRoot.resolve(storedFileName).normalize();
    }
}
