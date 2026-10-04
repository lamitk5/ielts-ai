package com.ieltsaitutor.rag.ingestion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.ieltsaitutor.rag.config.RagProperties;

class FileSystemDocumentStorageServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void usesGeneratedPathNotOriginalFilename() throws Exception {
        RagProperties properties = new RagProperties();
        properties.setStorageRoot(tempDir.toString());
        FileSystemDocumentStorageService storage = new FileSystemDocumentStorageService(properties,
                new DocumentFileValidator(properties), new DocumentChecksumService());

        StoredDocument stored = storage.store(new MockMultipartFile("file", "original-guide.txt", "text/plain",
                "content".getBytes()), UUID.randomUUID(), UUID.randomUUID());

        assertFalse(stored.relativePath().contains("original-guide.txt"));
        assertTrue(Files.exists(stored.absolutePath()));
    }

    @Test
    void writesBelowConfiguredRoot() throws Exception {
        RagProperties properties = new RagProperties();
        properties.setStorageRoot(tempDir.toString());
        FileSystemDocumentStorageService storage = new FileSystemDocumentStorageService(properties,
                new DocumentFileValidator(properties), new DocumentChecksumService());

        StoredDocument stored = storage.store(new MockMultipartFile("file", "guide.txt", "text/plain",
                "content".getBytes()), UUID.randomUUID(), UUID.randomUUID());

        assertTrue(stored.absolutePath().normalize().startsWith(tempDir.toAbsolutePath().normalize()));
    }
}
