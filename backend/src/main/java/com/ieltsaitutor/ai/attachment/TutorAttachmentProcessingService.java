package com.ieltsaitutor.ai.attachment;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
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
    private final TaskExecutor executor;

    public TutorAttachmentProcessingService(TutorAttachmentRepository repository,
            TutorAttachmentDocumentProcessor documents, TutorAttachmentChunkRepository chunks,
            EmbeddingProvider embeddings) {
        this(repository, documents, chunks, embeddings, Runnable::run);
    }

    @Autowired
    public TutorAttachmentProcessingService(TutorAttachmentRepository repository,
            TutorAttachmentDocumentProcessor documents, TutorAttachmentChunkRepository chunks,
            EmbeddingProvider embeddings,
            @Qualifier("tutorAttachmentProcessingExecutor") TaskExecutor executor) {
        this.repository = repository;
        this.processor = null;
        this.documents = documents;
        this.chunks = chunks;
        this.embeddings = embeddings;
        this.executor = executor;
    }

    public TutorAttachmentProcessingService(TutorAttachmentRepository repository, Consumer<TutorAttachment> processor) {
        this.repository = repository;
        this.processor = processor;
        this.documents = null;
        this.chunks = null;
        this.embeddings = null;
        this.executor = Runnable::run;
    }

    public void submit(UUID attachmentId) {
        try {
            executor.execute(() -> process(attachmentId));
        } catch (RejectedExecutionException exception) {
            repository.updateStatus(attachmentId, TutorAttachment.AttachmentStatus.FAILED,
                    "ATTACHMENT_PROCESSING_QUEUE_FULL");
        }
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
                String deferredEmbeddingCode = processDocument(attachment);
                repository.updateStatus(attachmentId, TutorAttachment.AttachmentStatus.READY, deferredEmbeddingCode);
                return;
            }
            repository.updateStatus(attachmentId, TutorAttachment.AttachmentStatus.READY, null);
        } catch (RuntimeException exception) {
            repository.updateStatus(attachmentId, TutorAttachment.AttachmentStatus.FAILED, failureCode(exception));
        }
    }

    private String processDocument(TutorAttachment attachment) {
        if (attachment.kind() == AttachmentKind.IMAGE) return null;
        if (documents == null || chunks == null || embeddings == null) {
            throw new IllegalStateException("ATTACHMENT_PROCESSOR_NOT_CONFIGURED");
        }
        List<TutorAttachmentChunk> extracted = documents.processDocument(attachment);
        if (extracted == null || extracted.isEmpty()) {
            throw new IllegalStateException("ATTACHMENT_EXTRACTION_EMPTY");
        }
        chunks.replace(attachment.id(), extracted, null, null);
        if (!embeddings.isEmbeddingConfigured()) {
            return "ATTACHMENT_EMBEDDING_DEFERRED";
        }
        try {
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
            return null;
        } catch (RagEmbeddingException exception) {
            if (!isDeferredEmbeddingFailure(exception)) throw exception;
            return exception.getCode();
        }
    }

    private boolean isDeferredEmbeddingFailure(RagEmbeddingException exception) {
        return exception.getStatus() == 429 || exception.getStatus() == 502
                || exception.getStatus() == 503 || exception.getStatus() == 504;
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
