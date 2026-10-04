package com.ieltsaitutor.speaking;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpeakingAudioStorageTest {

    private Path tempDir;
    private FileSystemSpeakingAudioStorage storage;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("speaking-audio-test-");
        storage = new FileSystemSpeakingAudioStorage(tempDir.toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (tempDir != null && Files.exists(tempDir)) {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                });
            }
        }
    }

    @Test
    @DisplayName("Stores audio securely under opaque server key and reads back bytes accurately")
    void storesAndRetrievesAudio() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        byte[] content = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };

        SpeakingAudioReference ref = storage.store(
                userId, submissionId, "audio/webm", new ByteArrayInputStream(content), content.length);

        assertNotNull(ref.storageKey());
        assertTrue(ref.storageKey().startsWith("speaking/"));
        assertFalse(ref.storageKey().contains("..")); // No path traversal
        assertEquals("audio/webm", ref.mimeType());
        assertEquals(8L, ref.sizeBytes());

        try (InputStream readStream = storage.read(ref.storageKey())) {
            byte[] readBytes = readStream.readAllBytes();
            assertArrayEquals(content, readBytes);
        }
    }

    @Test
    @DisplayName("Path traversal attempts in storageKey are safely blocked")
    void blocksPathTraversalOnRead() {
        assertThrows(IllegalArgumentException.class, () -> storage.read("../../../etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> storage.read("speaking/../../secret.txt"));
    }
}
