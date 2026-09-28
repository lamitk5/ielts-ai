package com.ieltsaitutor.ai.attachment;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.rag.embedding.EmbeddingProvider;
import com.ieltsaitutor.rag.embedding.EmbeddingRequest;
import com.ieltsaitutor.rag.embedding.EmbeddingResult;
import com.ieltsaitutor.rag.embedding.EmbeddingSpace;
import com.ieltsaitutor.rag.embedding.EmbeddingTask;
import com.ieltsaitutor.rag.embedding.RagEmbeddingException;

@Service
public class TutorAttachmentProcessingService {
    private final TutorAttachmentRepository repository;
    private final Consumer<TutorAttachment> processor;
    private final TutorAttachmentDocumentProcessor documents;
    private final TutorAttachmentChunkRepository chunks;
    private final EmbeddingProvider embeddings;

    @Autowired
    public TutorAttachmentProcessingService(TutorAttachmentRepository repository,
            TutorAttachmentDocumentProcessor documents, TutorAttachmentChunkRepository chunks,
            EmbeddingProvider embeddings) {
        this.repository = repository;
        this.processor = null;
        this.documents = documents;
        this.chunks = chunks;
        this.embeddings = embeddings;
    }

    public TutorAttachmentProcessingService(TutorAttachmentRepository repository, Consumer<TutorAttachment> processor) {
        this.repository = repository;
        this.processor = processor;
        this.documents = null;
        this.chunks = null;
        this.embeddings = null;
    }

    public void process(UUID attachmentId) {
        TutorAttachment attachment = repository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
        if (attachment.status() == TutorAttachment.AttachmentStatus.READY
                || attachment.status() == TutorAttachment.AttachmentStatus.REMOVED
                || attachment.status() == TutorAttachment.AttachmentStatus.EXPIRED) return;
        repository.updateStatus(attachmentId, TutorAttachment.AttachmentStatus.PROCESSING, null);
        try {
            if (processor != null) {
                processor.accept(attachment);
            } else {
                processDocument(attachment);
            }
            repository.updateStatus(attachmentId, TutorAttachment.AttachmentStatus.READY, null);
        } catch (RuntimeException exception) {
            repository.updateStatus(attachmentId, TutorAttachment.AttachmentStatus.FAILED, failureCode(exception));
        }
    }

    private void processDocument(TutorAttachment attachment) {
        if (attachment.kind() == AttachmentKind.IMAGE) return;
        if (documents == null || chunks == null || embeddings == null) {
            throw new IllegalStateException("ATTACHMENT_PROCESSOR_NOT_CONFIGURED");
        }
        List<TutorAttachmentChunk> extracted = documents.processDocument(attachment);
        if (extracted == null || extracted.isEmpty()) {
            throw new IllegalStateException("ATTACHMENT_EXTRACTION_EMPTY");
        }
        if (!embeddings.isEmbeddingConfigured()) {
            throw new IllegalStateException("ATTACHMENT_EMBEDDING_UNAVAILABLE");
        }
        EmbeddingSpace space = embeddings.embeddingSpace();
        if (space == null || space.dimension() != 768) {
            throw new IllegalStateException("ATTACHMENT_EMBEDDING_SPACE_INVALID");
        }
        List<EmbeddingResult> vectors = embeddings.embedBatch(extracted.stream()
                .map(chunk -> new EmbeddingRequest(chunk.content(), EmbeddingTask.DOCUMENT, space)).toList());
        if (vectors.size() != extracted.size() || vectors.stream().anyMatch(vector ->
                vector.dimension() != 768 || vector.space() == null || !space.matches(vector.space()))) {
            throw new IllegalStateException("ATTACHMENT_EMBEDDING_SPACE_INVALID");
        }
        chunks.replace(attachment.id(), extracted, vectors.stream().map(EmbeddingResult::values).toList(), space);
    }

    private String failureCode(RuntimeException exception) {
        if (exception instanceof TutorAttachmentDocumentProcessor.NeedsVisionException) {
            return "ATTACHMENT_NEEDS_VISION";
        }
        if (exception instanceof RagEmbeddingException embeddingException) {
            return embeddingException.getCode();
        }
        return exception.getMessage() != null && exception.getMessage().startsWith("ATTACHMENT_")
                ? exception.getMessage() : "ATTACHMENT_PROCESSING_FAILED";
    }
}
