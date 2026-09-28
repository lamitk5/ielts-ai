package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemTutorAttachmentStorageTest {
    @TempDir
    Path tempDir;

    @Test
    void usesGeneratedKeyBelowConfiguredRoot() throws Exception {
        FileSystemTutorAttachmentStorage storage = new FileSystemTutorAttachmentStorage(tempDir);

        storage.store(new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)), "a/one.bin", 7);

        assertThat(Files.exists(tempDir.resolve("a/one.bin"))).isTrue();
    }

    @Test
    void rejectsTraversalKey() {
        FileSystemTutorAttachmentStorage storage = new FileSystemTutorAttachmentStorage(tempDir);

        assertThatThrownBy(() -> storage.store(new ByteArrayInputStream(new byte[] { 1 }), "../outside.bin", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void opensStoredBytes() throws Exception {
        FileSystemTutorAttachmentStorage storage = new FileSystemTutorAttachmentStorage(tempDir);
        storage.store(new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)), "stored.bin", 7);

        try (var input = storage.open("stored.bin")) {
            assertThat(input.readAllBytes()).containsExactly("content".getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    void deletesStoredBytes() throws Exception {
        FileSystemTutorAttachmentStorage storage = new FileSystemTutorAttachmentStorage(tempDir);
        storage.store(new ByteArrayInputStream(new byte[] { 1, 2 }), "stored.bin", 2);

        storage.delete("stored.bin");

        assertThat(storage.exists("stored.bin")).isFalse();
    }

    @Test
    void neverUsesOriginalFilenameAsPath() throws Exception {
        FileSystemTutorAttachmentStorage storage = new FileSystemTutorAttachmentStorage(tempDir);
        storage.store(new ByteArrayInputStream(new byte[] { 1 }), "generated/uuid.bin", 1);

        assertThat(Files.exists(tempDir.resolve("original-guide.txt"))).isFalse();
        assertThat(storage.exists("generated/uuid.bin")).isTrue();
    }
}
