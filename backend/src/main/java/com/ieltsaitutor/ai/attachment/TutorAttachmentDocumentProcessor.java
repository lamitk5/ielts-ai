package com.ieltsaitutor.ai.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.rag.domain.ExtractionStatus;
import com.ieltsaitutor.rag.ingestion.ChunkingOptions;
import com.ieltsaitutor.rag.ingestion.DocumentChunk;
import com.ieltsaitutor.rag.ingestion.DocumentChunker;
import com.ieltsaitutor.rag.ingestion.DocumentExtractor;
import com.ieltsaitutor.rag.ingestion.ExtractionResult;
import com.ieltsaitutor.rag.ingestion.StoredDocument;

@Service
public class TutorAttachmentDocumentProcessor {
    private final TutorAttachmentStorage storage;
    private final DocumentExtractor extractor;
    private final DocumentChunker chunker;

    @Autowired
    public TutorAttachmentDocumentProcessor(TutorAttachmentStorage storage, DocumentExtractor extractor,
            DocumentChunker chunker) {
        this.storage = storage;
        this.extractor = extractor;
        this.chunker = chunker;
    }

    public List<TutorAttachmentChunk> processDocument(TutorAttachment attachment) {
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile("en-attachment-", ".bin");
            try (InputStream input = storage.open(attachment.storageKey())) {
                Files.copy(input, temporaryFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            StoredDocument stored = new StoredDocument(temporaryFile, temporaryFile.getFileName().toString(),
                    attachment.filename(), attachment.contentType(), attachment.sizeBytes(), attachment.sha256());
            ExtractionResult extraction = extractor.extract(stored);
            if (extraction.status() == ExtractionStatus.NEEDS_OCR) {
                throw new NeedsVisionException();
            }
            if (extraction.status() != ExtractionStatus.READY_FOR_REVIEW || extraction.document() == null) {
                throw new IllegalStateException("ATTACHMENT_EXTRACTION_FAILED");
            }
            return chunker.chunk(extraction.document(), ChunkingOptions.defaults()).stream()
                    .map(chunk -> map(attachment, chunk)).toList();
        } catch (NeedsVisionException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("ATTACHMENT_READ_FAILED", exception);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // Temporary files are isolated and can be reclaimed by the OS.
                }
            }
        }
    }

    private TutorAttachmentChunk map(TutorAttachment attachment, DocumentChunk chunk) {
        return new TutorAttachmentChunk(attachment.id(), attachment.filename(), chunk.pageNumber(),
                chunk.sectionTitle(), chunk.chunkIndex(), chunk.content(), chunk.tokenCount());
    }

    public static final class NeedsVisionException extends RuntimeException {
        public NeedsVisionException() {
            super("ATTACHMENT_NEEDS_VISION");
        }
    }
}
