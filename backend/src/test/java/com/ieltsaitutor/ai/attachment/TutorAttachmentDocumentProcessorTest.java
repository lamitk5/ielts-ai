package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.rag.ingestion.DocumentChunker;

class TutorAttachmentDocumentProcessorTest {
    private final TutorAttachmentStorage storage = mock(TutorAttachmentStorage.class);
    private final TutorAttachmentDocumentProcessor processor = new TutorAttachmentDocumentProcessor(
            storage, new com.ieltsaitutor.rag.ingestion.TikaDocumentExtractor(), new DocumentChunker());

    @Test
    void extractsTxtWithCharsetSafety() throws Exception {
        TutorAttachment attachment = attachment("notes.txt", "text/plain", "Học IELTS đúng cách".getBytes(StandardCharsets.UTF_8));

        List<TutorAttachmentChunk> chunks = process(attachment);

        assertThat(chunks).singleElement().satisfies(chunk -> {
            assertThat(chunk.content()).contains("Học IELTS");
            assertThat(chunk.attachmentId()).isEqualTo(attachment.id());
            assertThat(chunk.filename()).isEqualTo("notes.txt");
        });
    }

    @Test
    void extractsDocxText() throws Exception {
        TutorAttachment attachment = attachment("lesson.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes("DOCX lesson"));

        assertThat(process(attachment)).extracting(TutorAttachmentChunk::content).anyMatch(text -> text.contains("DOCX lesson"));
    }

    @Test
    void extractsPdfTextWithPageMetadata() throws Exception {
        TutorAttachment attachment = attachment("lesson.pdf", "application/pdf", pdfBytes("PDF lesson"));

        assertThat(process(attachment)).singleElement().satisfies(chunk -> {
            assertThat(chunk.content()).contains("PDF lesson");
            assertThat(chunk.pageNumber()).isEqualTo(1);
        });
    }

    @Test
    void returnsNeedsVisionForScannedPdf() throws Exception {
        TutorAttachment attachment = attachment("scan.pdf", "application/pdf", "%PDF-1.4\n%%EOF".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> process(attachment))
                .isInstanceOf(TutorAttachmentDocumentProcessor.NeedsVisionException.class)
                .hasMessage("ATTACHMENT_NEEDS_VISION");
    }

    @Test
    void preservesChunkProvenance() throws Exception {
        TutorAttachment attachment = attachment("reading.txt", "text/plain", "Passage content\n\nQuestion context".getBytes(StandardCharsets.UTF_8));

        TutorAttachmentChunk chunk = process(attachment).get(0);

        assertThat(chunk.attachmentId()).isEqualTo(attachment.id());
        assertThat(chunk.filename()).isEqualTo("reading.txt");
        assertThat(chunk.chunkIndex()).isZero();
        assertThat(chunk.sectionLabel()).isNull();
        assertThat(chunk.tokenEstimate()).isPositive();
    }

    private List<TutorAttachmentChunk> process(TutorAttachment attachment) throws Exception {
        when(storage.open(attachment.storageKey())).thenReturn(new java.io.ByteArrayInputStream(
                attachmentBytes.get(attachment.id())));
        return processor.processDocument(attachment);
    }

    private final java.util.Map<UUID, byte[]> attachmentBytes = new java.util.HashMap<>();

    private TutorAttachment attachment(String filename, String contentType, byte[] bytes) {
        UUID id = UUID.randomUUID();
        attachmentBytes.put(id, bytes);
        Instant now = Instant.now();
        return new TutorAttachment(id, UUID.randomUUID(), UUID.randomUUID(), filename, filename, contentType,
                AttachmentKind.DOCUMENT, bytes.length, "sha", "key-" + id, TutorAttachment.AttachmentStatus.STORED,
                null, null, 0, now, now, null);
    }

    private static byte[] docxBytes(String text) throws IOException {
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
