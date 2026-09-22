package com.ieltsaitutor.rag.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.ieltsaitutor.rag.config.RagProperties;
import com.ieltsaitutor.rag.domain.ExtractionStatus;
import com.ieltsaitutor.rag.domain.IndexStatus;
import com.ieltsaitutor.rag.domain.IngestionJobStatus;
import com.ieltsaitutor.rag.domain.RagDocument;
import com.ieltsaitutor.rag.domain.RagDocumentVersion;
import com.ieltsaitutor.rag.domain.RagIngestionJob;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.domain.Skill;
import com.ieltsaitutor.rag.embedding.EmbeddingProvider;
import com.ieltsaitutor.rag.embedding.EmbeddingRequest;
import com.ieltsaitutor.rag.embedding.EmbeddingResult;
import com.ieltsaitutor.rag.repository.RagChunkRepository;
import com.ieltsaitutor.rag.repository.RagDocumentRepository;
import com.ieltsaitutor.rag.repository.RagDocumentVersionRepository;
import com.ieltsaitutor.rag.repository.RagIngestionJobRepository;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {
    @Mock RagDocumentRepository documents;
    @Mock RagDocumentVersionRepository versions;
    @Mock RagIngestionJobRepository jobs;
    @Mock RagChunkRepository chunks;
    @Mock DocumentStorageService storage;
    @Mock DocumentExtractor extractor;
    @Mock DocumentChunker chunker;
    @Mock EmbeddingProvider embeddings;
    @Mock DocumentChecksumService checksums;

    private RagProperties properties;
    private DefaultDocumentIngestionService service;

    @BeforeEach
    void setUp() {
        properties = new RagProperties();
        properties.setEmbeddingDimension(2);
        service = new DefaultDocumentIngestionService(documents, versions, jobs, chunks, storage, extractor,
                chunker, embeddings, checksums, properties);
    }

    @Test
    void uploadsAsPendingReview() {
        MockMultipartFile file = new MockMultipartFile("file", "guide.pdf", "application/pdf", "content".getBytes());
        StoredDocument stored = stored("checksum");
        when(checksums.sha256(any())).thenReturn("checksum");
        when(versions.findByChecksum("checksum")).thenReturn(Optional.empty());
        when(storage.store(any(), any(), any())).thenReturn(stored);

        UploadReceipt receipt = service.upload(new UploadCommand("Guide", "UPLOAD", "author", "org", "en",
                Skill.GENERAL, file));

        assertThat(receipt.rightsStatus()).isEqualTo(RightsStatus.PENDING_REVIEW);
        assertThat(receipt.indexStatus()).isEqualTo(IndexStatus.NOT_INDEXED);
        verify(documents).create(any(RagDocument.class));
        verify(versions).createVersion(any(RagDocumentVersion.class));
        verify(jobs).createJob(any(RagIngestionJob.class));
    }

    @Test
    void createsDocumentBeforeLinkingCurrentVersion() {
        MockMultipartFile file = new MockMultipartFile("file", "guide.pdf", "application/pdf", "content".getBytes());
        when(checksums.sha256(any())).thenReturn("checksum");
        when(versions.findByChecksum("checksum")).thenReturn(Optional.empty());
        when(storage.store(any(), any(), any())).thenReturn(stored("checksum"));

        service.upload(new UploadCommand("Guide", "UPLOAD", "author", "org", "en", Skill.GENERAL, file));

        ArgumentCaptor<RagDocument> captor = ArgumentCaptor.forClass(RagDocument.class);
        verify(documents).create(captor.capture());
        assertThat(captor.getValue().currentVersionId()).isNull();
        verify(documents).setCurrentVersion(any(), any());
    }

    @Test
    void extractsPreviewWithoutApproval() {
        UUID documentId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        when(versions.findById(versionId)).thenReturn(Optional.of(version(documentId, versionId, null,
                ExtractionStatus.PENDING, IndexStatus.NOT_INDEXED)));
        when(extractor.extract(any())).thenReturn(ExtractionResult.ready(
                new ExtractedDocument("preview text", List.of(new ExtractedSection("Heading", "preview text", 1, Map.of())), Map.of())));

        DocumentPreview preview = service.extractPreview(documentId, versionId);

        assertThat(preview.text()).isEqualTo("preview text");
        verify(versions).updateExtractionStatus(versionId, ExtractionStatus.READY_FOR_REVIEW);
    }

    @Test
    void resolvesStoredDocumentPathAgainstConfiguredStorageRoot() {
        properties.setStorageRoot("target/rag-test-uploads");
        UUID documentId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        when(versions.findById(versionId)).thenReturn(Optional.of(version(documentId, versionId, null,
                ExtractionStatus.PENDING, IndexStatus.NOT_INDEXED)));
        when(extractor.extract(any())).thenReturn(ExtractionResult.ready(
                new ExtractedDocument("preview text", List.of(new ExtractedSection(null, "preview text", null, Map.of())), Map.of())));

        service.extractPreview(documentId, versionId);

        ArgumentCaptor<StoredDocument> captor = ArgumentCaptor.forClass(StoredDocument.class);
        verify(extractor).extract(captor.capture());
        assertThat(captor.getValue().absolutePath()).isEqualTo(properties.storageRootPath().toAbsolutePath().normalize()
                .resolve("doc/version/guide.pdf").normalize());
    }

    @Test
    void refusesIndexBeforeApproval() {
        UUID documentId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        when(documents.findById(documentId)).thenReturn(Optional.of(document(documentId, RightsStatus.PENDING_REVIEW, false, versionId)));

        assertThatThrownBy(() -> service.index(documentId, versionId, IndexMode.SYNC))
                .isInstanceOf(RagInvalidStateException.class);
    }

    @Test
    void marksNeedsOcrWithoutChunks() {
        UUID documentId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        approvedInputs(documentId, versionId);
        when(extractor.extract(any())).thenReturn(ExtractionResult.needsOcr("needs OCR"));

        IndexReceipt receipt = service.index(documentId, versionId, IndexMode.SYNC);

        assertThat(receipt.indexStatus()).isEqualTo(IndexStatus.NOT_INDEXED);
        verify(chunks, never()).insertBatch(any());
        verify(versions).updateExtractionStatus(versionId, ExtractionStatus.NEEDS_OCR);
    }

    @Test
    void leavesVersionNonIndexedAfterEmbeddingFailure() {
        UUID documentId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        approvedInputs(documentId, versionId);
        when(extractor.extract(any())).thenReturn(readyExtraction());
        when(chunker.chunk(any(), any())).thenReturn(List.of(new DocumentChunk(0, "text", 1, "Heading", 1, Map.of())));
        when(embeddings.embedBatch(any())).thenThrow(new RuntimeException("provider down"));

        assertThatThrownBy(() -> service.index(documentId, versionId, IndexMode.SYNC))
                .isInstanceOf(RagInvalidStateException.class);
        verify(versions).updateIndexStatus(versionId, IndexStatus.FAILED);
        verify(chunks, never()).insertBatch(any());
    }

    @Test
    void rejectsDimensionMismatchDuringIndex() {
        UUID documentId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        approvedInputs(documentId, versionId);
        when(extractor.extract(any())).thenReturn(readyExtraction());
        when(chunker.chunk(any(), any())).thenReturn(List.of(new DocumentChunk(0, "text", 1, "Heading", 1, Map.of())));
        when(embeddings.embedBatch(any())).thenReturn(List.of(new EmbeddingResult("model", 1, List.of(1f))));

        assertThatThrownBy(() -> service.index(documentId, versionId, IndexMode.SYNC))
                .isInstanceOf(RagInvalidStateException.class);
        verify(versions).updateIndexStatus(versionId, IndexStatus.FAILED);
    }

    @Test
    void avoidsDuplicateChecksumVersion() {
        UUID existing = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "guide.pdf", "application/pdf", "content".getBytes());
        when(checksums.sha256(any())).thenReturn("checksum");
        when(versions.findByChecksum("checksum")).thenReturn(Optional.of(version(UUID.randomUUID(), existing, "checksum",
                ExtractionStatus.READY_FOR_REVIEW, IndexStatus.NOT_INDEXED)));

        UploadReceipt receipt = service.upload(new UploadCommand("Guide", "UPLOAD", null, null, "en", Skill.GENERAL, file));

        assertThat(receipt.versionId()).isEqualTo(existing);
        verify(storage, never()).store(any(), any(), any());
    }

    private void approvedInputs(UUID documentId, UUID versionId) {
        when(documents.findById(documentId)).thenReturn(Optional.of(document(documentId, RightsStatus.APPROVED, false, versionId)));
        when(versions.findById(versionId)).thenReturn(Optional.of(version(documentId, versionId, "checksum",
                ExtractionStatus.READY_FOR_REVIEW, IndexStatus.NOT_INDEXED)));
        when(extractor.extract(any())).thenReturn(readyExtraction());
    }

    private ExtractionResult readyExtraction() {
        return ExtractionResult.ready(new ExtractedDocument("text", List.of(
                new ExtractedSection("Heading", "text", 1, Map.of())), Map.of()));
    }

    private StoredDocument stored(String checksum) {
        return new StoredDocument(Path.of("guide.pdf"), "doc/version/guide.pdf", "guide.pdf", "application/pdf", 7, checksum);
    }

    private RagDocument document(UUID id, RightsStatus rightsStatus, boolean active, UUID currentVersion) {
        Instant now = Instant.now();
        return new RagDocument(id, "Guide", "UPLOAD", null, null, "en", Skill.GENERAL, rightsStatus, "note", active,
                currentVersion, now, now);
    }

    private RagDocumentVersion version(UUID documentId, UUID id, String checksum, ExtractionStatus extraction, IndexStatus index) {
        Instant now = Instant.now();
        return new RagDocumentVersion(id, documentId, "1", "guide.pdf", "application/pdf", 7,
                checksum == null ? "checksum" : checksum, "doc/version/guide.pdf", extraction, index, now, now, now);
    }
}
