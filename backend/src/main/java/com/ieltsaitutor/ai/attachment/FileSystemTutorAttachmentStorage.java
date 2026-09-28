package com.ieltsaitutor.ai.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileSystemTutorAttachmentStorage implements TutorAttachmentStorage {
    private final Path root;

    public FileSystemTutorAttachmentStorage(
            @Value("${ai.tutor.attachment.storage-root:backend/data/tutor-attachments}") String root) {
        this(Path.of(root));
    }

    public FileSystemTutorAttachmentStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public void store(InputStream input, String storageKey, long sizeBytes) throws IOException {
        if (input == null || sizeBytes < 0) {
            throw new IllegalArgumentException("Attachment input is invalid.");
        }
        Path target = resolve(storageKey);
        Files.createDirectories(target.getParent());
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        return Files.newInputStream(resolve(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.exists(resolve(storageKey));
    }

    private Path resolve(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || storageKey.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Attachment storage key is invalid.");
        }
        Path relative = Path.of(storageKey).normalize();
        if (relative.isAbsolute() || relative.startsWith("..")) {
            throw new IllegalArgumentException("Attachment storage key escapes the configured root.");
        }
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Attachment storage key escapes the configured root.");
        }
        return target;
    }
}
