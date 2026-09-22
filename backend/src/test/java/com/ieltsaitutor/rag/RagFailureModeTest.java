package com.ieltsaitutor.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.ieltsaitutor.rag.chat.DefaultGroundingValidator;
import com.ieltsaitutor.rag.chat.DefaultRagChatService;
import com.ieltsaitutor.rag.chat.RagContextBuilder;
import com.ieltsaitutor.rag.config.RagProperties;
import com.ieltsaitutor.rag.domain.ExtractionStatus;
import com.ieltsaitutor.rag.domain.IndexStatus;
import com.ieltsaitutor.rag.domain.RagDocument;
import com.ieltsaitutor.rag.domain.RagDocumentVersion;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.domain.Skill;
import com.ieltsaitutor.rag.embedding.EmbeddingProvider;
import com.ieltsaitutor.rag.embedding.EmbeddingResult;
import com.ieltsaitutor.rag.ingestion.DefaultDocumentIngestionService;
import com.ieltsaitutor.rag.ingestion.DocumentChecksumService;
import com.ieltsaitutor.rag.ingestion.DocumentChunk;
import com.ieltsaitutor.rag.ingestion.DocumentChunker;
import com.ieltsaitutor.rag.ingestion.DocumentExtractor;
import com.ieltsaitutor.rag.ingestion.DocumentStorageService;
import com.ieltsaitutor.rag.ingestion.ExtractedDocument;
import com.ieltsaitutor.rag.ingestion.ExtractedSection;
import com.ieltsaitutor.rag.ingestion.ExtractionResult;
import com.ieltsaitutor.rag.ingestion.IndexMode;
import com.ieltsaitutor.rag.ingestion.RagInvalidStateException;
import com.ieltsaitutor.rag.ingestion.StoredDocument;
import com.ieltsaitutor.rag.ingestion.UploadCommand;
import com.ieltsaitutor.rag.repository.RagChunkRepository;
import com.ieltsaitutor.rag.repository.RagDocumentRepository;
import com.ieltsaitutor.rag.repository.RagDocumentVersionRepository;
import com.ieltsaitutor.rag.repository.RagIngestionJobRepository;
import com.ieltsaitutor.rag.retrieval.RagQuery;
import com.ieltsaitutor.rag.retrieval.VectorRetrievalService;
import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.provider.AiProvider;

@ExtendWith(MockitoExtension.class)
@Tag("rag-postgres")
class RagFailureModeTest {
    @Mock RagDocumentRepository documents;
    @Mock RagDocumentVersionRepository versions;
    @Mock RagIngestionJobRepository jobs;
    @Mock RagChunkRepository chunks;
    @Mock DocumentStorageService storage;
    @Mock DocumentExtractor extractor;
    @Mock DocumentChunker chunker;
    @Mock EmbeddingProvider embeddings;

    private RagProperties properties;

    @BeforeEach
    void setUp() { properties = new RagProperties(); properties.setEmbeddingDimension(2); }

    @Test
    void dimensionMismatchFailsJob() {
        UUID documentId = UUID.randomUUID(), versionId = UUID.randomUUID();
        approvedInputs(documentId, versionId);
        when(embeddings.embedBatch(any())).thenReturn(List.of(new EmbeddingResult("test", 1, List.of(1f))));
        assertThatThrownBy(() -> service().index(documentId, versionId, IndexMode.SYNC)).isInstanceOf(RagInvalidStateException.class)
                .hasMessageContaining("Embedding dimension");
        verify(versions).updateIndexStatus(versionId, IndexStatus.FAILED);
    }

    @Test
    void duplicateChecksumIsIdempotent() {
        UUID existingVersion = UUID.randomUUID();
        when(versions.findByChecksum(any())).thenReturn(Optional.of(version(existingVersion, UUID.randomUUID(), null, IndexStatus.INDEXED)));
        var receipt = service().upload(new UploadCommand("Guide", "UPLOAD", null, null, "en", Skill.GENERAL,
                new MockMultipartFile("file", "guide.txt", "text/plain", "content".getBytes())));
        assertThat(receipt.duplicate()).isTrue();
        assertThat(receipt.versionId()).isEqualTo(existingVersion);
    }

    @Test
    void embeddingFailureLeavesVersionNonIndexed() {
        UUID documentId = UUID.randomUUID(), versionId = UUID.randomUUID();
        approvedInputs(documentId, versionId);
        when(embeddings.embedBatch(any())).thenThrow(new RuntimeException("provider down"));
        assertThatThrownBy(() -> service().index(documentId, versionId, IndexMode.SYNC)).isInstanceOf(RagInvalidStateException.class);
        verify(versions).updateIndexStatus(versionId, IndexStatus.FAILED);
        verify(chunks, never()).insertBatch(any());
    }

    @Test
    void scannedPdfNeedsOcr() {
        UUID documentId = UUID.randomUUID(), versionId = UUID.randomUUID();
        approvedInputs(documentId, versionId);
        when(extractor.extract(any())).thenReturn(ExtractionResult.needsOcr("scanned PDF"));
        var receipt = service().index(documentId, versionId, IndexMode.SYNC);
        assertThat(receipt.indexStatus()).isEqualTo(IndexStatus.NOT_INDEXED);
        verify(chunks, never()).insertBatch(any());
        verify(versions).updateExtractionStatus(versionId, ExtractionStatus.NEEDS_OCR);
    }

    @Test
    void weakSimilarityReturnsInsufficientEvidence() {
        AiProvider provider = org.mockito.Mockito.mock(AiProvider.class);
        VectorRetrievalService retrieval = org.mockito.Mockito.mock(VectorRetrievalService.class);
        when(retrieval.search(any(RagQuery.class))).thenReturn(List.of());
        var result = new DefaultRagChatService(provider, retrieval, org.mockito.Mockito.mock(RagContextBuilder.class), new DefaultGroundingValidator())
                .chat(new AiChatCommand("Explain this rubric", new AiChatContext("WRITING", null, null, null, null, null, null), List.of()));
        assertThat(result.status()).isEqualTo("INSUFFICIENT_CONTEXT");
        assertThat(result.sources()).isEmpty();
    }

    private DefaultDocumentIngestionService service() {
        return new DefaultDocumentIngestionService(documents, versions, jobs, chunks, storage, extractor, chunker,
                embeddings, new DocumentChecksumService(), properties);
    }

    private void approvedInputs(UUID documentId, UUID versionId) {
        when(documents.findById(documentId)).thenReturn(Optional.of(new RagDocument(documentId, "Guide", "UPLOAD", null, null,
                "en", Skill.GENERAL, RightsStatus.APPROVED, "licensed", false, versionId, Instant.now(), Instant.now())));
        when(versions.findById(versionId)).thenReturn(Optional.of(version(versionId, documentId, Instant.now(), IndexStatus.NOT_INDEXED)));
        when(extractor.extract(any())).thenReturn(ExtractionResult.ready(new ExtractedDocument("text",
                List.of(new ExtractedSection("Guide", "text", 1, Map.of())), Map.of())));
        lenient().when(chunker.chunk(any(), any())).thenReturn(List.of(new DocumentChunk(0, "text", 1, "Guide", 1, Map.of())));
    }

    private RagDocumentVersion version(UUID versionId, UUID documentId, Instant approvedAt, IndexStatus indexStatus) {
        return new RagDocumentVersion(versionId, documentId, "1", "guide.txt", "text/plain", 7, "checksum",
                "guide.txt", ExtractionStatus.READY_FOR_REVIEW, indexStatus, approvedAt, null, Instant.now());
    }
}
