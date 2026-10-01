package com.ieltsaitutor.rag.ingestion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.ieltsaitutor.rag.config.RagProperties;

class DocumentFileValidatorTest {
    private final RagProperties properties = new RagProperties();
    private final DocumentFileValidator validator = new DocumentFileValidator(properties);

    @Test
    void rejectsOversizedUpload() {
        properties.setMaxUploadBytes(3);
        MockMultipartFile file = new MockMultipartFile("file", "guide.txt", "text/plain", "toolong".getBytes());

        assertThrows(RagValidationException.class, () -> validator.validate(file));
    }

    @Test
    void rejectsPathTraversal() {
        MockMultipartFile file = new MockMultipartFile("file", "../guide.txt", "text/plain", "ok".getBytes());

        assertThrows(RagValidationException.class, () -> validator.validate(file));
    }

    @Test
    void rejectsUnsupportedMime() {
        MockMultipartFile file = new MockMultipartFile("file", "guide.exe", "application/octet-stream", "MZ".getBytes());

        assertThrows(RagValidationException.class, () -> validator.validate(file));
    }

    @Test
    void acceptsPdfDocxAndTxt() {
        assertDoesNotThrow(() -> validator.validate(new MockMultipartFile("file", "guide.txt", "text/plain", "ok".getBytes())));
        assertDoesNotThrow(() -> validator.validate(new MockMultipartFile("file", "guide.pdf", "application/pdf", "%PDF".getBytes())));
        assertDoesNotThrow(() -> validator.validate(new MockMultipartFile("file", "guide.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "PK".getBytes())));
    }

    @Test
    void calculatesStableChecksum() {
        DocumentChecksumService checksum = new DocumentChecksumService();
        String first = checksum.sha256(new ByteArrayInputStream("same".getBytes(StandardCharsets.UTF_8)));
        String second = checksum.sha256(new ByteArrayInputStream("same".getBytes(StandardCharsets.UTF_8)));

        org.junit.jupiter.api.Assertions.assertEquals(first, second);
        org.junit.jupiter.api.Assertions.assertEquals(64, first.length());
    }
}
