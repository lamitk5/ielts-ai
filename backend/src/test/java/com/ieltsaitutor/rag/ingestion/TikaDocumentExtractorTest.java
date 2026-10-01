package com.ieltsaitutor.rag.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.ieltsaitutor.rag.domain.ExtractionStatus;

class TikaDocumentExtractorTest {
    @TempDir
    Path tempDir;

    private final TikaDocumentExtractor extractor = new TikaDocumentExtractor();

    @Test
    void extractsTxt() throws Exception {
        ExtractionResult result = extractor.extract(stored("guide.txt", "text/plain", "Heading\nUseful text".getBytes()));

        assertEquals(ExtractionStatus.READY_FOR_REVIEW, result.status());
        assertTrue(result.document().text().contains("Useful text"));
    }

    @Test
    void extractsDocx() throws Exception {
        ExtractionResult result = extractor.extract(stored("guide.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxBytes("Docx lesson")));

        assertEquals(ExtractionStatus.READY_FOR_REVIEW, result.status());
        assertTrue(result.document().text().contains("Docx lesson"));
    }

    @Test
    void extractsPdf() throws Exception {
        ExtractionResult result = extractor.extract(stored("guide.pdf", "application/pdf", pdfBytes("PDF lesson")));

        assertEquals(ExtractionStatus.READY_FOR_REVIEW, result.status());
    }

    @Test
    void returnsNeedsOcrForEmptyPdf() throws Exception {
        ExtractionResult result = extractor.extract(stored("scan.pdf", "application/pdf", "%PDF-1.4\n%%EOF".getBytes()));

        assertEquals(ExtractionStatus.NEEDS_OCR, result.status());
    }

    @Test
    void rejectsMalformedDocument() throws Exception {
        ExtractionResult result = extractor.extract(stored("broken.pdf", "application/pdf", "not a pdf".getBytes()));

        assertEquals(ExtractionStatus.FAILED, result.status());
    }

    private StoredDocument stored(String name, String mime, byte[] bytes) throws IOException {
        Path path = tempDir.resolve(UUID.randomUUID() + ".bin");
        Files.write(path, bytes);
        return new StoredDocument(path, path.getFileName().toString(), name, mime, bytes.length, "checksum");
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
