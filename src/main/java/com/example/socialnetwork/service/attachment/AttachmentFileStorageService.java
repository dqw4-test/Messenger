package com.example.socialnetwork.service.attachment;

import com.example.socialnetwork.config.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttachmentFileStorageService {
    private final StorageProperties storageProperties;

    public String store(String extension, InputStream inputStream) throws IOException {
        Path root = storageRoot();
        Files.createDirectories(root);
        String key = UUID.randomUUID() + (extension == null || extension.isBlank() ? "" : "." + extension);
        Path target = root.resolve(key).normalize();
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        return key;
    }

    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(storageRoot().resolve(storageKey).normalize());
    }

    public InputStream open(String storageKey) throws IOException {
        return Files.newInputStream(storageRoot().resolve(storageKey).normalize());
    }

    private Path storageRoot() {
        String uploadDir = storageProperties.getUploadDir();
        if (uploadDir == null || uploadDir.isBlank()) {
            uploadDir = "uploads";
        }
        return Path.of(uploadDir).toAbsolutePath().normalize();
    }
}
