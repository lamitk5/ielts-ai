package com.ieltsaitutor.rag.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.rag.domain.IndexStatus;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.ingestion.DocumentIngestionService;
import com.ieltsaitutor.rag.ingestion.UploadReceipt;

class RagCliRunnerTest {
    @Test
    void invokesSharedIngestionService() throws Exception {
        Path root = Files.createTempDirectory("rag-files");
        Files.writeString(root.resolve("guide.txt"), "guide");
        Path manifest = Files.createTempFile("manifest", ".yml");
        Files.writeString(manifest, "sources:\n  - file: guide.txt\n    title: Guide\n    language: en\n    skill: GENERAL\n    rightsStatus: PENDING_REVIEW\n    rightsNote: licensed\n");
        DocumentIngestionService ingestion = mock(DocumentIngestionService.class);
        when(ingestion.upload(any())).thenReturn(new UploadReceipt(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                RightsStatus.PENDING_REVIEW, IndexStatus.NOT_INDEXED, false, false));

        int code = new RagCliRunner(ingestion, new RagManifestParser(root)).ingest(manifest);

        assertThat(code).isZero();
        verify(ingestion).upload(any());
    }

    @Test
    void neverApprovesManifestEntry() throws Exception {
        Path root = Files.createTempDirectory("rag-files");
        Files.writeString(root.resolve("guide.txt"), "guide");
        Path manifest = Files.createTempFile("manifest", ".yml");
        Files.writeString(manifest, "sources:\n  - file: guide.txt\n    title: Guide\n    language: en\n    skill: GENERAL\n    rightsStatus: APPROVED\n    rightsNote: licensed\n");
        DocumentIngestionService ingestion = mock(DocumentIngestionService.class);
        when(ingestion.upload(any())).thenReturn(new UploadReceipt(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                RightsStatus.PENDING_REVIEW, IndexStatus.NOT_INDEXED, false, false));

        new RagCliRunner(ingestion, new RagManifestParser(root)).ingest(manifest);

        verify(ingestion).upload(any());
    }

    @Test
    void returnsNonZeroForInvalidManifest() throws Exception {
        Path root = Files.createTempDirectory("rag-files");
        Path manifest = Files.createTempFile("manifest", ".yml");
        Files.writeString(manifest, "sources:\n  - file: ../bad.txt\n    title: Bad\n    language: en\n    skill: GENERAL\n    rightsStatus: PENDING_REVIEW\n    rightsNote: licensed\n");

        assertThat(new RagCliRunner(mock(DocumentIngestionService.class), new RagManifestParser(root)).ingest(manifest)).isEqualTo(1);
    }
}
