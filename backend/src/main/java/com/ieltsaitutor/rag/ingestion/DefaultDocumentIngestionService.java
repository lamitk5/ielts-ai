package com.ieltsaitutor.rag.ingestion;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ieltsaitutor.rag.config.RagProperties;
import com.ieltsaitutor.rag.domain.ExtractionStatus;
import com.ieltsaitutor.rag.domain.IndexStatus;
import com.ieltsaitutor.rag.domain.IngestionJobStatus;
import com.ieltsaitutor.rag.domain.RagChunk;
import com.ieltsaitutor.rag.domain.RagDocument;
import com.ieltsaitutor.rag.domain.RagDocumentVersion;
import com.ieltsaitutor.rag.domain.RagIngestionJob;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.embedding.EmbeddingProvider;
import com.ieltsaitutor.rag.embedding.EmbeddingRequest;
import com.ieltsaitutor.rag.embedding.EmbeddingResult;
import com.ieltsaitutor.rag.embedding.EmbeddingTask;
import com.ieltsaitutor.rag.repository.RagChunkRepository;
import com.ieltsaitutor.rag.repository.RagDocumentRepository;
import com.ieltsaitutor.rag.repository.RagDocumentVersionRepository;
import com.ieltsaitutor.rag.repository.RagIngestionJobRepository;

@Service
public class DefaultDocumentIngestionService implements DocumentIngestionService {
    private final RagDocumentRepository documents;
    private final RagDocumentVersionRepository versions;
    private final RagIngestionJobRepository jobs;
    private final RagChunkRepository chunks;
    private final DocumentStorageService storage;
    private final DocumentExtractor extractor;
    private final DocumentChunker chunker;
    private final EmbeddingProvider embeddings;
    private final DocumentChecksumService checksums;
    private final RagProperties properties;

    public DefaultDocumentIngestionService(RagDocumentRepository documents, RagDocumentVersionRepository versions,
            RagIngestionJobRepository jobs, RagChunkRepository chunks, DocumentStorageService storage,
            DocumentExtractor extractor, DocumentChunker chunker, EmbeddingProvider embeddings,
            DocumentChecksumService checksums, RagProperties properties) {
        this.documents = documents;
        this.versions = versions;
        this.jobs = jobs;
        this.chunks = chunks;
        this.storage = storage;
        this.extractor = extractor;
        this.chunker = chunker;
        this.embeddings = embeddings;
        this.checksums = checksums;
        this.properties = properties;
    }

    @Override
    @Transactional
    public UploadReceipt upload(UploadCommand command) {
        String checksum = checksum(command);
        var duplicate = versions.findByChecksum(checksum);
        if (duplicate.isPresent()) {
            RagDocumentVersion version = duplicate.get();
            return new UploadReceipt(version.documentId(), version.id(), null, RightsStatus.PENDING_REVIEW,
                    version.indexStatus(), false, true);
        }

        UUID documentId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        StoredDocument stored = storage.store(command.file(), documentId, versionId);
        Instant now = Instant.now();
        RagDocument document = new RagDocument(documentId, command.title(), command.sourceType(), command.author(),
                command.organization(), command.language(), command.skill(), RightsStatus.PENDING_REVIEW, "", false,
                null, now, now);
        RagDocumentVersion version = new RagDocumentVersion(versionId, documentId, "1", stored.originalFilename(),
                stored.mimeType(), stored.fileSizeBytes(), stored.checksum(), stored.relativePath(),
                ExtractionStatus.PENDING, IndexStatus.NOT_INDEXED, null, null, now);
        RagIngestionJob job = new RagIngestionJob(jobId, documentId, versionId, IngestionJobStatus.PENDING, null, null,
                null, null, now);
        documents.create(document);
        versions.createVersion(version);
        documents.setCurrentVersion(documentId, versionId);
        jobs.createJob(job);
        return new UploadReceipt(documentId, versionId, jobId, RightsStatus.PENDING_REVIEW, IndexStatus.NOT_INDEXED, false,
                false);
    }

    @Override
    @Transactional
    public DocumentPreview extractPreview(UUID documentId, UUID versionId) {
        RagDocumentVersion version = requireVersion(documentId, versionId);
        ExtractionResult result = extractor.extract(storedDocument(version));
        versions.updateExtractionStatus(versionId, result.status());
        if (result.status() != ExtractionStatus.READY_FOR_REVIEW || result.preview() == null) {
            throw invalid(result.errorCode() == null ? "RAG_EXTRACTION_FAILED" : result.errorCode(),
                    result.errorMessage() == null ? "Không thể trích xuất tài liệu." : result.errorMessage());
        }
        return result.preview();
    }

    @Override
    @Transactional
    public IndexReceipt index(UUID documentId, UUID versionId, IndexMode mode) {
        var document = documents.findById(documentId)
                .orElseThrow(() -> invalid("RAG_DOCUMENT_NOT_FOUND", "Không tìm thấy tài liệu."));
        RagDocumentVersion version = requireVersion(documentId, versionId);
        if (document.rightsStatus() != RightsStatus.APPROVED || !versionId.equals(document.currentVersionId())
                || version.approvedAt() == null) {
            throw invalid("RAG_INDEX_NOT_ALLOWED", "Tài liệu phải được duyệt và là version hiện tại trước khi index.");
        }
        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();
        RagIngestionJob job = new RagIngestionJob(jobId, documentId, versionId, IngestionJobStatus.INDEXING, null, null,
                now, null, now);
        jobs.createJob(job);
        versions.updateIndexStatus(versionId, IndexStatus.INDEXING);
        try {
            ExtractionResult extraction = extractor.extract(storedDocument(version));
            versions.updateExtractionStatus(versionId, extraction.status());
            if (extraction.status() == ExtractionStatus.NEEDS_OCR) {
                RagIngestionJob finished = finish(job, IngestionJobStatus.NEEDS_OCR, extraction.errorCode(), extraction.errorMessage());
                jobs.updateStatus(finished);
                versions.updateIndexStatus(versionId, IndexStatus.NOT_INDEXED);
                return new IndexReceipt(versionId, jobId, 0, IndexStatus.NOT_INDEXED);
            }
            if (extraction.status() != ExtractionStatus.READY_FOR_REVIEW || extraction.document() == null) {
                throw invalid("RAG_EXTRACTION_FAILED", extraction.errorMessage());
            }
            List<DocumentChunk> documentChunks = chunker.chunk(extraction.document(), ChunkingOptions.defaults());
            List<EmbeddingResult> vectors = embeddings.embedBatch(documentChunks.stream()
                    .map(chunk -> new EmbeddingRequest(chunk.content(), EmbeddingTask.DOCUMENT)).toList());
            if (vectors.size() != documentChunks.size()) {
                throw invalid("RAG_EMBEDDING_COUNT_MISMATCH", "Số vector không khớp số chunk.");
            }
            List<RagChunk> persisted = toChunks(documentChunks, vectors, versionId);
            chunks.insertBatch(persisted);
            versions.updateIndexStatus(versionId, IndexStatus.INDEXED);
            versions.setIndexedAt(versionId, Instant.now());
            jobs.updateStatus(finish(job, IngestionJobStatus.INDEXED, null, null));
            return new IndexReceipt(versionId, jobId, persisted.size(), IndexStatus.INDEXED);
        } catch (Exception exception) {
            versions.updateIndexStatus(versionId, IndexStatus.FAILED);
            jobs.updateStatus(finish(job, IngestionJobStatus.FAILED, code(exception), safeMessage(exception)));
            if (exception instanceof RagInvalidStateException invalid) throw invalid;
            throw invalid("RAG_INDEX_FAILED", "Không thể hoàn tất indexing tài liệu.", exception);
        }
    }

    private List<RagChunk> toChunks(List<DocumentChunk> chunks, List<EmbeddingResult> vectors, UUID versionId) {
        for (EmbeddingResult vector : vectors) {
            if (vector.dimension() != properties.embeddingDimension() || vector.values().size() != properties.embeddingDimension()) {
                throw invalid("RAG_EMBEDDING_DIMENSION_MISMATCH", "Embedding dimension không khớp cấu hình.");
            }
        }
        Instant now = Instant.now();
        return java.util.stream.IntStream.range(0, chunks.size()).mapToObj(index -> {
            DocumentChunk chunk = chunks.get(index);
            EmbeddingResult vector = vectors.get(index);
            return new RagChunk(UUID.randomUUID(), versionId, chunk.chunkIndex(), chunk.content(), chunk.pageNumber(),
                    chunk.sectionTitle(), chunk.tokenCount(), vector.values(), chunk.metadata(), now);
        }).toList();
    }

    private RagDocumentVersion requireVersion(UUID documentId, UUID versionId) {
        RagDocumentVersion version = versions.findById(versionId)
                .orElseThrow(() -> invalid("RAG_VERSION_NOT_FOUND", "Không tìm thấy version."));
        if (!documentId.equals(version.documentId())) throw invalid("RAG_VERSION_MISMATCH", "Version không thuộc tài liệu.");
        return version;
    }

    private StoredDocument storedDocument(RagDocumentVersion version) {
        return new StoredDocument(java.nio.file.Path.of(version.storagePath()), version.storagePath(),
                version.originalFilename(), version.mimeType(), version.fileSizeBytes(), version.checksum());
    }

    private String checksum(UploadCommand command) {
        try (var input = command.file().getInputStream()) { return checksums.sha256(input); }
        catch (IOException exception) { throw invalid("RAG_UPLOAD_READ_FAILED", "Không thể đọc file upload.", exception); }
    }

    private RagIngestionJob finish(RagIngestionJob job, IngestionJobStatus status, String code, String message) {
        return new RagIngestionJob(job.id(), job.documentId(), job.documentVersionId(), status, code, message,
                job.startedAt(), Instant.now(), job.createdAt());
    }

    private String code(Exception exception) {
        return exception instanceof RagInvalidStateException invalid ? invalid.getCode() : "RAG_INDEX_FAILED";
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null ? "Indexing failed." : exception.getMessage();
    }

    private RagInvalidStateException invalid(String code, String message) { return invalid(code, message, null); }
    private RagInvalidStateException invalid(String code, String message, Throwable cause) {
        return cause == null ? new RagInvalidStateException(code, message) : new RagInvalidStateException(code, message, cause);
    }
}
