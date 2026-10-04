package com.ieltsaitutor.speaking;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileSystemSpeakingAudioStorage implements SpeakingAudioStorage {

    private final Path rootDir;

    public FileSystemSpeakingAudioStorage(
            @Value("${app.speaking.audio.storage-dir:data/speaking-audio}") String storageDir) {
        this.rootDir = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create audio storage root directory", e);
        }
    }

    @Override
    public SpeakingAudioReference store(
            UUID userId, UUID submissionId, String mimeType, InputStream content, long sizeBytes) throws IOException {

        String fileId = UUID.randomUUID().toString();
        String storageKey = "speaking/" + userId + "/" + submissionId + "/" + fileId + ".bin";
        Path targetPath = resolvePath(storageKey);

        Files.createDirectories(targetPath.getParent());

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }

        try (var output = Files.newOutputStream(targetPath)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = content.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
        }

        String checksum = HexFormat.of().formatHex(digest.digest());
        return new SpeakingAudioReference(storageKey, mimeType, sizeBytes, checksum);
    }

    @Override
    public InputStream read(String storageKey) throws IOException {
        Path path = resolvePath(storageKey);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Audio file not found: " + storageKey);
        }
        return Files.newInputStream(path);
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path path = resolvePath(storageKey);
            Files.deleteIfExists(path);
        } catch (Exception ignored) {}
    }

    private Path resolvePath(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || storageKey.contains("..")) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        Path resolved = rootDir.resolve(storageKey).normalize();
        if (!resolved.startsWith(rootDir)) {
            throw new IllegalArgumentException("Path traversal attempt detected");
        }
        return resolved;
    }
}
