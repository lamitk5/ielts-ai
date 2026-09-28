package com.ieltsaitutor.rag.ingestion;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipFile;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.rag.domain.ExtractionStatus;

@Service
public class TikaDocumentExtractor implements DocumentExtractor {
    @Override
    public ExtractionResult extract(StoredDocument storedDocument) {
        boolean pdf = storedDocument.mimeType() != null && storedDocument.mimeType().toLowerCase(Locale.ROOT).contains("pdf");
        boolean docx = storedDocument.mimeType() != null && storedDocument.mimeType().toLowerCase(Locale.ROOT).contains("wordprocessingml");
        if (pdf && !startsWithPdf(storedDocument)) {
            return ExtractionResult.failed("Không thể đọc nội dung tài liệu.");
        }
        try (InputStream input = Files.newInputStream(storedDocument.absolutePath())) {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, storedDocument.originalFilename());
            new AutoDetectParser().parse(input, handler, metadata, new org.apache.tika.parser.ParseContext());
            String text = handler.toString().trim();
            if (text.isBlank() && docx) {
                text = extractDocxXml(storedDocument);
            }
            if (text.isBlank()) {
                return pdf ? ExtractionResult.needsOcr("PDF không có văn bản có thể trích xuất; cần OCR ở phase sau.")
                        : ExtractionResult.failed("Tài liệu không có văn bản có thể trích xuất.");
            }
            ExtractedSection section = new ExtractedSection(null, text, pdf ? 1 : null,
                    Map.of("mimeType", storedDocument.mimeType()));
            return ExtractionResult.ready(new ExtractedDocument(text, List.of(section), Map.of("mimeType", storedDocument.mimeType())));
        } catch (Exception exception) {
            if (pdf && startsWithPdf(storedDocument)) {
                return ExtractionResult.needsOcr("PDF không có văn bản có thể trích xuất; cần OCR ở phase sau.");
            }
            if (docx) {
                String text = extractDocxXml(storedDocument);
                if (!text.isBlank()) {
                    ExtractedSection section = new ExtractedSection(null, text, null,
                            Map.of("mimeType", storedDocument.mimeType()));
                    return ExtractionResult.ready(new ExtractedDocument(text, List.of(section), Map.of("mimeType", storedDocument.mimeType())));
                }
            }
            return new ExtractionResult(ExtractionStatus.FAILED, null, null, "RAG_EXTRACTION_FAILED",
                    "Không thể đọc nội dung tài liệu.");
        }
    }

    private String extractDocxXml(StoredDocument storedDocument) {
        try (ZipFile zip = new ZipFile(storedDocument.absolutePath().toFile())) {
            var entry = zip.getEntry("word/document.xml");
            if (entry == null) {
                return "";
            }
            try (InputStream input = zip.getInputStream(entry)) {
                return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                        .replaceAll("<[^>]+>", " ").replace("&amp;", "&").trim();
            }
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean startsWithPdf(StoredDocument storedDocument) {
        try (InputStream input = Files.newInputStream(storedDocument.absolutePath())) {
            byte[] prefix = input.readNBytes(5);
            return new String(prefix, java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF");
        } catch (Exception ignored) {
            return false;
        }
    }
}
