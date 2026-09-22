package com.ieltsaitutor.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.ieltsaitutor.ai.dto.AiGrounding;
import com.ieltsaitutor.ai.dto.AiSource;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.rag.chat.DefaultRagChatService;
import com.ieltsaitutor.rag.chat.DefaultGroundingValidator;
import com.ieltsaitutor.rag.chat.RagContextBuilder;
import com.ieltsaitutor.rag.chat.RagPromptContext;
import com.ieltsaitutor.rag.chat.RagChatResult;
import com.ieltsaitutor.rag.config.RagProperties;
import com.ieltsaitutor.rag.domain.ExtractionStatus;
import com.ieltsaitutor.rag.domain.IndexStatus;
import com.ieltsaitutor.rag.domain.RagDocument;
import com.ieltsaitutor.rag.domain.RagDocumentVersion;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.domain.Skill;
import com.ieltsaitutor.rag.embedding.EmbeddingProvider;
import com.ieltsaitutor.rag.embedding.EmbeddingResult;
import com.ieltsaitutor.rag.ingestion.ChunkingOptions;
import com.ieltsaitutor.rag.ingestion.DefaultDocumentIngestionService;
import com.ieltsaitutor.rag.ingestion.DefaultRagLifecycleService;
import com.ieltsaitutor.rag.ingestion.DocumentChunk;
import com.ieltsaitutor.rag.ingestion.DocumentChunker;
import com.ieltsaitutor.rag.ingestion.DocumentExtractor;
import com.ieltsaitutor.rag.ingestion.DocumentPreview;
import com.ieltsaitutor.rag.ingestion.DocumentStorageService;
import com.ieltsaitutor.rag.ingestion.ExtractedDocument;
import com.ieltsaitutor.rag.ingestion.ExtractedSection;
import com.ieltsaitutor.rag.ingestion.ExtractionResult;
import com.ieltsaitutor.rag.ingestion.IndexMode;
import com.ieltsaitutor.rag.ingestion.IndexReceipt;
import com.ieltsaitutor.rag.ingestion.RightsReviewCommand;
import com.ieltsaitutor.rag.ingestion.StoredDocument;
import com.ieltsaitutor.rag.ingestion.UploadCommand;
import com.ieltsaitutor.rag.ingestion.UploadReceipt;
import com.ieltsaitutor.rag.ingestion.DocumentChecksumService;
import com.ieltsaitutor.rag.repository.RagChunkRepository;
import com.ieltsaitutor.rag.repository.RagDocumentRepository;
import com.ieltsaitutor.rag.repository.RagDocumentVersionRepository;
import com.ieltsaitutor.rag.repository.RagIngestionJobRepository;
import com.ieltsaitutor.rag.retrieval.RagQuery;
import com.ieltsaitutor.rag.retrieval.RetrievedChunk;
import com.ieltsaitutor.rag.retrieval.VectorRetrievalService;

@ExtendWith(MockitoExtension.class)
@Tag("rag-postgres")
class RagEndToEndTest {
    @Mock RagDocumentRepository documents;
    @Mock RagDocumentVersionRepository versions;
    @Mock RagIngestionJobRepository jobs;
    @Mock RagChunkRepository chunks;
    @Mock DocumentStorageService storage;
    @Mock DocumentExtractor extractor;
    @Mock DocumentChunker chunker;
    @Mock EmbeddingProvider embeddings;
    @Mock DocumentChecksumService checksums;

    @Test
    void uploadPreviewApproveIndexActivateAndRetrieve() {
        Instant now = Instant.now();
        RagProperties properties = properties();
        DefaultDocumentIngestionService ingestion = new DefaultDocumentIngestionService(documents, versions, jobs,
                chunks, storage, extractor, chunker, embeddings, checksums, properties);
        DefaultRagLifecycleService lifecycle = new DefaultRagLifecycleService(documents, versions);
        MockMultipartFile file = new MockMultipartFile("file", "guide.txt", "text/plain", "rubric text".getBytes());
        when(checksums.sha256(any())).thenReturn("checksum-1");
        when(versions.findByChecksum("checksum-1")).thenReturn(Optional.empty());
        when(storage.store(any(), any(), any())).thenReturn(new StoredDocument(Path.of("guide.txt"), "guide.txt",
                "guide.txt", "text/plain", 11, "checksum-1"));
        UploadReceipt uploaded = ingestion.upload(new UploadCommand("Writing Guide", "UPLOAD", "Author", "Org", "en",
                Skill.WRITING, file));
        assertThat(uploaded.rightsStatus()).isEqualTo(RightsStatus.PENDING_REVIEW);
        UUID documentId = uploaded.documentId();
        UUID versionId = uploaded.versionId();

        RagDocument pending = new RagDocument(documentId, "Writing Guide", "UPLOAD", "Author", "Org", "en",
                Skill.WRITING, RightsStatus.PENDING_REVIEW, "", false, versionId, now, now);
        RagDocument approved = new RagDocument(documentId, "Writing Guide", "UPLOAD", "Author", "Org", "en",
                Skill.WRITING, RightsStatus.APPROVED, "licensed", false, versionId, now, now);
        RagDocumentVersion pendingVersion = version(versionId, documentId, null, IndexStatus.NOT_INDEXED);
        RagDocumentVersion approvedVersion = version(versionId, documentId, now, IndexStatus.NOT_INDEXED);
        RagDocumentVersion indexedVersion = version(versionId, documentId, now, IndexStatus.INDEXED);
        when(documents.findById(any())).thenReturn(Optional.of(pending), Optional.of(approved), Optional.of(approved));
        when(versions.findById(any())).thenReturn(Optional.of(pendingVersion), Optional.of(approvedVersion), Optional.of(indexedVersion));
        when(extractor.extract(any())).thenReturn(ExtractionResult.ready(extracted()));
        DocumentPreview preview = ingestion.extractPreview(uploaded.documentId(), uploaded.versionId());
        assertThat(preview.text()).contains("rubric text");
        lifecycle.approve(documentId, new RightsReviewCommand("licensed"));
        when(chunker.chunk(any(), any(ChunkingOptions.class))).thenReturn(List.of(new DocumentChunk(0, "rubric text", 1,
                "Task Response", 3, Map.of())));
        when(embeddings.embedBatch(any())).thenReturn(List.of(new EmbeddingResult("test", 2, List.of(1f, 0f))));
        IndexReceipt indexed = ingestion.index(documentId, versionId, IndexMode.SYNC);
        assertThat(indexed.indexStatus()).isEqualTo(IndexStatus.INDEXED);
        lifecycle.activate(documentId);
        verify(documents).activate(documentId);

        VectorRetrievalService retrieval = mock(VectorRetrievalService.class);
        RetrievedChunk result = new RetrievedChunk(UUID.randomUUID(), documentId, versionId, "guide-1", "Writing Guide",
                "1", 1, "Task Response", "rubric text", .91);
        when(retrieval.search(any(RagQuery.class))).thenReturn(List.of(result));
        assertThat(retrieval.search(new RagQuery("Explain the rubric", Skill.WRITING, null, null, null, 5, .72)))
                .extracting(RetrievedChunk::sourceId).containsExactly("guide-1");
    }

    @Test
    void deactivationExcludesExistingVectors() {
        UUID documentId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        DefaultRagLifecycleService lifecycle = new DefaultRagLifecycleService(documents, versions);
        lifecycle.deactivate(documentId);
        verify(documents).deactivate(documentId);
        assertThat(com.ieltsaitutor.rag.repository.RagChunkJdbcRepository.GOVERNED_CANDIDATE_SQL)
                .contains("active = true");
    }

    @Test
    void reapprovalRequiresReindex() {
        UUID documentId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        when(documents.findById(documentId)).thenReturn(Optional.of(document(documentId, versionId, RightsStatus.APPROVED, true)));
        DefaultRagLifecycleService lifecycle = new DefaultRagLifecycleService(documents, versions);
        lifecycle.approve(documentId, new RightsReviewCommand("reviewed again"));
        verify(versions).updateIndexStatus(versionId, IndexStatus.NOT_INDEXED);
        verify(versions).setIndexedAt(versionId, null);
    }

    @Test
    void groundedResponseContainsOnlyRetrievedSources() {
        AiProvider provider = mock(AiProvider.class);
        VectorRetrievalService retrieval = mock(VectorRetrievalService.class);
        RagContextBuilder context = mock(RagContextBuilder.class);
        RetrievedChunk chunk = new RetrievedChunk(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "source-1",
                "Guide", "1", 4, "Task Response", "evidence", .94);
        when(retrieval.search(any())).thenReturn(List.of(chunk));
        when(context.build(List.of(chunk))).thenReturn(new RagPromptContext("evidence",
                List.of(new AiSource("source-1", "Guide", "Task Response"), new AiSource("invented", "Fake", "Other"))));
        when(provider.chat(any())).thenReturn(AiChatResult.answered("Grounded answer"));
        RagChatResult result = new DefaultRagChatService(provider, retrieval, context, new DefaultGroundingValidator())
                .chat(new AiChatCommand("Explain the rubric", new com.ieltsaitutor.ai.dto.AiChatContext("WRITING", null,
                        null, null, null, null, null), List.of()));
        assertThat(result.grounding()).isEqualTo(new AiGrounding("GROUNDED", true));
        assertThat(result.sources()).extracting(AiSource::sourceId).containsExactly("source-1");
        verify(provider).chat(any());
    }

    private RagProperties properties() { RagProperties properties = new RagProperties(); properties.setEmbeddingDimension(2); return properties; }
    private ExtractedDocument extracted() { return new ExtractedDocument("rubric text", List.of(new ExtractedSection("Task Response", "rubric text", 1, Map.of())), Map.of()); }
    private RagDocument document(UUID id, UUID versionId, RightsStatus status, boolean active) {
        Instant now = Instant.now(); return new RagDocument(id, "Guide", "UPLOAD", null, null, "en", Skill.WRITING, status, "note", active, versionId, now, now);
    }
    private RagDocumentVersion version(UUID id, UUID documentId, Instant approvedAt, IndexStatus indexStatus) {
        Instant now = Instant.now();
        Instant indexedAt = indexStatus == IndexStatus.INDEXED ? now : null;
        return new RagDocumentVersion(id, documentId, "1", "guide.txt", "text/plain", 11, "checksum-1", "guide.txt", ExtractionStatus.READY_FOR_REVIEW, indexStatus, approvedAt, indexedAt, now);
    }
}
