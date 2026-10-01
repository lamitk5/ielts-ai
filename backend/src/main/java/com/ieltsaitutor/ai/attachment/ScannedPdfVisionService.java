package com.ieltsaitutor.ai.attachment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.ai.exception.AiProviderException;
import com.ieltsaitutor.ai.model.AiAttachmentPart;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.ai.provider.ProviderCapability;

@Service
public class ScannedPdfVisionService {
    private static final int BATCH_SIZE = 5;
    private final TutorAttachmentStorage storage;
    private final AiProvider provider;
    private final TutorAttachmentDocumentProcessor documents;
    private final PdfPageRenderer renderer;

    public ScannedPdfVisionService(TutorAttachmentStorage storage, AiProvider provider,
            TutorAttachmentDocumentProcessor documents, PdfPageRenderer renderer) {
        this.storage = storage;
        this.provider = provider;
        this.documents = documents;
        this.renderer = renderer;
    }

    public VisionContextResult analyzeScannedPdf(TutorAttachment attachment, String question) {
        try {
            List<TutorAttachmentChunk> text = documents.processDocument(attachment);
            String answer = text.stream().map(TutorAttachmentChunk::content).collect(java.util.stream.Collectors.joining("\n\n"));
            return new VisionContextResult(VisionContextResult.Status.TEXT_READY, answer, List.of(), 0, null, null);
        } catch (TutorAttachmentDocumentProcessor.NeedsVisionException needsVision) {
            return analyzeRenderedPages(attachment, question);
        } catch (RuntimeException exception) {
            return new VisionContextResult(VisionContextResult.Status.ERROR, "", List.of(), 0, null,
                    "ATTACHMENT_VISION_FAILED");
        }
    }

    private VisionContextResult analyzeRenderedPages(TutorAttachment attachment, String question) {
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile("en-scanned-pdf-", ".pdf");
            try (InputStream input = storage.open(attachment.storageKey())) {
                Files.copy(input, temporaryFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            int totalPages;
            try (PDDocument document = Loader.loadPDF(temporaryFile.toFile())) {
                totalPages = document.getNumberOfPages();
            }
            int renderCount = Math.min(totalPages, PdfPageRenderer.MAX_RENDERED_PAGES);
            List<RenderedAttachmentPage> pages = new ArrayList<>();
            StringBuilder answer = new StringBuilder();
            for (int start = 1; start <= renderCount; start += BATCH_SIZE) {
                int end = Math.min(renderCount, start + BATCH_SIZE - 1);
                List<RenderedAttachmentPage> batch = renderer.renderRelevantPages(temporaryFile, new PageRange(start, end)).stream()
                        .map(page -> new RenderedAttachmentPage(attachment.id(), page.pageNumber(), page.mediaType(), page.imageBytes()))
                        .toList();
                List<AiAttachmentPart> parts = batch.stream().map(page -> new AiAttachmentPart(attachment.id(),
                        attachment.filename() + "#page-" + page.pageNumber(), page.mediaType(), AttachmentKind.IMAGE,
                        () -> new ByteArrayInputStream(page.imageBytes()))).toList();
                AiChatResult result = provider.chat(new AiChatCommand(question, null, List.of(), UUID.randomUUID().toString(),
                        "Scanned PDF pages: " + start + "-" + end, parts, java.util.Set.of(ProviderCapability.VISION_IMAGE)));
                if (answer.length() > 0) answer.append("\n\n");
                answer.append(result.answer());
                pages.addAll(batch);
            }
            int remaining = Math.max(0, totalPages - renderCount);
            return new VisionContextResult(VisionContextResult.Status.ANSWERED, answer.toString(), pages, remaining,
                    remaining == 0 ? null : "ATTACHMENT_PAGE_LIMIT_REACHED", null);
        } catch (AiProviderException exception) {
            return new VisionContextResult(VisionContextResult.Status.ERROR, "", List.of(), 0, null,
                    "AI_VISION_UNAVAILABLE");
        } catch (IOException | RuntimeException exception) {
            return new VisionContextResult(VisionContextResult.Status.ERROR, "", List.of(), 0, null,
                    "ATTACHMENT_VISION_FAILED");
        } finally {
            if (temporaryFile != null) {
                try { Files.deleteIfExists(temporaryFile); } catch (IOException ignored) { }
            }
        }
    }
}
