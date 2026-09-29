package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.ieltsaitutor.ai.provider.ProviderId;
import com.ieltsaitutor.rag.embedding.EmbeddingProvider;
import com.ieltsaitutor.rag.embedding.EmbeddingRequest;
import com.ieltsaitutor.rag.embedding.EmbeddingResult;
import com.ieltsaitutor.rag.embedding.EmbeddingSpace;
import com.ieltsaitutor.rag.embedding.RagEmbeddingException;
import com.ieltsaitutor.rag.ingestion.DocumentChunker;
import com.ieltsaitutor.rag.ingestion.TikaDocumentExtractor;

class TutorAttachmentProcessingIntegrationTest {
    private static final EmbeddingSpace SPACE = new EmbeddingSpace(ProviderId.GEMINI, "gemini-embedding-2", 768, "v1");

    @Test
    void txtProcessingPersistsExtractedPhraseBeforeReady() throws Exception {
        assertProcessed("notes.txt", "text/plain", "LUMEN-TXT-UNIQUE-92841".getBytes(StandardCharsets.UTF_8),
                "LUMEN-TXT-UNIQUE-92841");
    }

    @Test
    void docxProcessingPersistsExtractedPhraseBeforeReady() throws Exception {
        assertProcessed("notes.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes("LUMEN-DOCX-UNIQUE-57319"), "LUMEN-DOCX-UNIQUE-57319");
    }

    @Test
    void pdfProcessingPersistsExtractedPhraseBeforeReady() throws Exception {
        assertProcessed("notes.pdf", "application/pdf", pdfBytes("LUMEN-PDF-UNIQUE-64127"),
                "LUMEN-PDF-UNIQUE-64127");
    }

    @Test
    void txtEmbeddingRateLimitKeepsAttachmentReadyWithPersistedText() throws Exception {
        assertReadyWhenEmbeddingFails("notes.txt", "text/plain",
                "LUMEN-TXT-DEFERRED-92841".getBytes(StandardCharsets.UTF_8),
                new RagEmbeddingException("RAG_EMBEDDING_RATE_LIMITED", 429, "rate limited"));
    }

    @Test
    void docxEmbeddingRateLimitKeepsAttachmentReadyWithPersistedText() throws Exception {
        assertReadyWhenEmbeddingFails("notes.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes("LUMEN-DOCX-DEFERRED-57319"),
                new RagEmbeddingException("RAG_EMBEDDING_RATE_LIMITED", 429, "rate limited"));
    }

    @Test
    void pdfEmbeddingUnavailableKeepsAttachmentReadyWithPersistedText() throws Exception {
        assertReadyWhenEmbeddingFails("notes.pdf", "application/pdf", pdfBytes("LUMEN-PDF-DEFERRED-64127"),
                new RagEmbeddingException("RAG_EMBEDDING_UNAVAILABLE", 503, "unavailable"));
    }

    @Test
    void embeddingTimeoutKeepsAttachmentReadyAndRecordsDeferredFailure() throws Exception {
        assertReadyWhenEmbeddingFails("notes.txt", "text/plain", "timeout content".getBytes(StandardCharsets.UTF_8),
                new RagEmbeddingException("RAG_EMBEDDING_UNAVAILABLE", 504, "timeout"));
    }

    @Test
    void validImageBecomesReadyWithoutCallingEmbeddingProvider() throws Exception {
        TutorAttachmentRepository repository = mock(TutorAttachmentRepository.class);
        TutorAttachmentStorage storage = mock(TutorAttachmentStorage.class);
        TutorAttachmentChunkRepository chunks = mock(TutorAttachmentChunkRepository.class);
        EmbeddingProvider embeddings = embeddingProvider();
        TutorAttachment attachment = attachment("photo.png", "image/png", new byte[] { 1, 2, 3 }, AttachmentKind.IMAGE);
        when(repository.findById(attachment.id())).thenReturn(Optional.of(attachment));

        TutorAttachmentProcessingService service = newProcessingService(repository, storage, chunks, embeddings);
        service.process(attachment.id());

        verify(embeddings, org.mockito.Mockito.never()).embedBatch(anyList());
        verify(chunks, org.mockito.Mockito.never()).replace(org.mockito.ArgumentMatchers.any(), anyList(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(repository).updateStatus(attachment.id(), TutorAttachment.AttachmentStatus.READY, null);
    }

    @Test
    void extractionFailureNeverBecomesReady() throws Exception {
        TutorAttachmentRepository repository = mock(TutorAttachmentRepository.class);
        TutorAttachmentStorage storage = mock(TutorAttachmentStorage.class);
        TutorAttachmentChunkRepository chunks = mock(TutorAttachmentChunkRepository.class);
        EmbeddingProvider embeddings = embeddingProvider();
        TutorAttachment attachment = attachment("broken.pdf", "application/pdf", new byte[0]);
        when(repository.findById(attachment.id())).thenReturn(Optional.of(attachment));
        when(storage.open(attachment.storageKey())).thenReturn(new ByteArrayInputStream("not a pdf".getBytes(StandardCharsets.UTF_8)));

        TutorAttachmentProcessingService service = newProcessingService(repository, storage, chunks, embeddings);
        service.process(attachment.id());

        verify(repository).updateStatus(attachment.id(), TutorAttachment.AttachmentStatus.FAILED, "ATTACHMENT_EXTRACTION_FAILED");
        verify(chunks, org.mockito.Mockito.never()).replace(org.mockito.ArgumentMatchers.any(), anyList(), anyList(), eq(SPACE));
    }

    private void assertProcessed(String filename, String contentType, byte[] bytes, String phrase) throws Exception {
        TutorAttachmentRepository repository = mock(TutorAttachmentRepository.class);
        TutorAttachmentStorage storage = mock(TutorAttachmentStorage.class);
        TutorAttachmentChunkRepository chunks = mock(TutorAttachmentChunkRepository.class);
        EmbeddingProvider embeddings = embeddingProvider();
        TutorAttachment attachment = attachment(filename, contentType, bytes);
        when(repository.findById(attachment.id())).thenReturn(Optional.of(attachment));
        when(storage.open(attachment.storageKey())).thenReturn(new ByteArrayInputStream(bytes));

        TutorAttachmentProcessingService service = newProcessingService(repository, storage, chunks, embeddings);
        service.process(attachment.id());

        ArgumentCaptor<List<TutorAttachmentChunk>> captured = ArgumentCaptor.forClass(List.class);
        verify(chunks).replace(eq(attachment.id()), captured.capture(), anyList(), eq(SPACE));
        assertThat(captured.getValue()).isNotEmpty();
        assertThat(captured.getValue()).extracting(TutorAttachmentChunk::content).anyMatch(text -> text.contains(phrase));
        verify(repository).updateStatus(attachment.id(), TutorAttachment.AttachmentStatus.PROCESSING, null);
        verify(repository).updateStatus(attachment.id(), TutorAttachment.AttachmentStatus.READY, null);
    }

    private void assertReadyWhenEmbeddingFails(String filename, String contentType, byte[] bytes,
            RagEmbeddingException failure) throws Exception {
        TutorAttachmentRepository repository = mock(TutorAttachmentRepository.class);
        TutorAttachmentStorage storage = mock(TutorAttachmentStorage.class);
        TutorAttachmentChunkRepository chunks = mock(TutorAttachmentChunkRepository.class);
        EmbeddingProvider embeddings = embeddingProviderThrowing(failure);
        TutorAttachment attachment = attachment(filename, contentType, bytes);
        when(repository.findById(attachment.id())).thenReturn(Optional.of(attachment));
        when(storage.open(attachment.storageKey())).thenReturn(new ByteArrayInputStream(bytes));

        TutorAttachmentProcessingService service = newProcessingService(repository, storage, chunks, embeddings);
        service.process(attachment.id());

        verify(chunks).replace(eq(attachment.id()), anyList(), isNull(), isNull());
        verify(repository).updateStatus(attachment.id(), TutorAttachment.AttachmentStatus.READY, failure.getCode());
        verify(repository, org.mockito.Mockito.never()).updateStatus(attachment.id(),
                TutorAttachment.AttachmentStatus.FAILED, failure.getCode());
    }

    private static TutorAttachmentProcessingService newProcessingService(TutorAttachmentRepository repository,
            TutorAttachmentStorage storage, TutorAttachmentChunkRepository chunks, EmbeddingProvider embeddings) {
        try {
            Constructor<TutorAttachmentProcessingService> constructor = TutorAttachmentProcessingService.class
                    .getConstructor(TutorAttachmentRepository.class, TutorAttachmentDocumentProcessor.class,
                            TutorAttachmentChunkRepository.class, EmbeddingProvider.class);
            return constructor.newInstance(repository,
                    new TutorAttachmentDocumentProcessor(storage, new TikaDocumentExtractor(), new DocumentChunker()),
                    chunks, embeddings);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("real attachment processor wiring is missing", exception);
        }
    }

    private static EmbeddingProvider embeddingProvider() {
        EmbeddingProvider embeddings = mock(EmbeddingProvider.class);
        when(embeddings.isEmbeddingConfigured()).thenReturn(true);
        when(embeddings.embeddingSpace()).thenReturn(SPACE);
        when(embeddings.embedBatch(anyList())).thenAnswer(invocation -> ((List<EmbeddingRequest>) invocation.getArgument(0)).stream()
                .map(ignored -> new EmbeddingResult(SPACE.model(), SPACE.dimension(), vector(), SPACE)).toList());
        return embeddings;
    }

    private static EmbeddingProvider embeddingProviderThrowing(RagEmbeddingException failure) {
        EmbeddingProvider embeddings = mock(EmbeddingProvider.class);
        when(embeddings.isEmbeddingConfigured()).thenReturn(true);
        when(embeddings.embeddingSpace()).thenReturn(SPACE);
        when(embeddings.embedBatch(anyList())).thenThrow(failure);
        return embeddings;
    }

    private static List<Float> vector() {
        return java.util.stream.IntStream.range(0, 768).mapToObj(index -> (float) index / 768).toList();
    }

    private static TutorAttachment attachment(String filename, String contentType, byte[] bytes) {
        return attachment(filename, contentType, bytes, AttachmentKind.DOCUMENT);
    }

    private static TutorAttachment attachment(String filename, String contentType, byte[] bytes, AttachmentKind kind) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        return new TutorAttachment(id, UUID.randomUUID(), UUID.randomUUID(), filename, filename, contentType,
                kind, bytes.length, "sha-" + id, "key-" + id,
                TutorAttachment.AttachmentStatus.STORED, null, null, 0, now, now, null);
    }

    private static byte[] docxBytes(String text) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body><w:p><w:r><w:t>"
                    + text + "</w:t></w:r></w:p></w:body></w:document>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    private static byte[] pdfBytes(String text) {
        return ("%PDF-1.4\n"
                + "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
                + "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n"
                + "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n"
                + "4 0 obj << /Length 42 >> stream\nBT /F1 12 Tf 72 720 Td (" + text + ") Tj ET\nendstream endobj\n"
                + "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n"
                + "trailer << /Root 1 0 R >>\n%%EOF").getBytes(StandardCharsets.UTF_8);
    }
}
