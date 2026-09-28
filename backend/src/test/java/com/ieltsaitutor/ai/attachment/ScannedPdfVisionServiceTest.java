package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.exception.AiProviderException;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.ai.provider.ProviderCapability;
import com.ieltsaitutor.ai.provider.ProviderId;
import com.ieltsaitutor.rag.ingestion.DocumentChunker;
import com.ieltsaitutor.rag.ingestion.TikaDocumentExtractor;

class ScannedPdfVisionServiceTest {
    private final TutorAttachmentStorage storage = mock(TutorAttachmentStorage.class);
    private final AiProvider provider = mock(AiProvider.class);
    private final TutorAttachmentDocumentProcessor documents = new TutorAttachmentDocumentProcessor(storage,
            new TikaDocumentExtractor(), new DocumentChunker());
    private final PdfPageRenderer renderer = new PdfPageRenderer();
    private final ScannedPdfVisionService service = new ScannedPdfVisionService(storage, provider, documents, renderer);

    @Test
    void textPdfSkipsVision() throws Exception {
        TutorAttachment attachment = attachment(pdfBytes(1, true));
        stubStorage(attachment);

        VisionContextResult result = service.analyzeScannedPdf(attachment, "What does it say?");

        assertThat(result.status()).isEqualTo(VisionContextResult.Status.TEXT_READY);
        assertThat(result.pages()).isEmpty();
        verifyNoInteractions(provider);
    }

    @Test
    void emptyPdfRendersActualPages() throws Exception {
        TutorAttachment attachment = attachment(pdfBytes(2, false));
        stubStorage(attachment);
        when(provider.chat(any())).thenReturn(AiChatResult.answered("The page is blank."));

        VisionContextResult result = service.analyzeScannedPdf(attachment, "What is visible?");

        assertThat(result.status()).isEqualTo(VisionContextResult.Status.ANSWERED);
        assertThat(result.pages()).hasSize(2).extracting(RenderedAttachmentPage::pageNumber).containsExactly(1, 2);
        assertThat(result.answer()).contains("blank");
    }

    @Test
    void capsRenderedPagesAtTwenty() throws Exception {
        TutorAttachment attachment = attachment(pdfBytes(25, false));
        stubStorage(attachment);
        when(provider.chat(any())).thenReturn(AiChatResult.answered("seen"));

        VisionContextResult result = service.analyzeScannedPdf(attachment, "Summarize pages");

        assertThat(result.pages()).hasSize(20);
        assertThat(result.remainingPageCount()).isEqualTo(5);
        assertThat(result.warning()).isEqualTo("ATTACHMENT_PAGE_LIMIT_REACHED");
    }

    @Test
    void batchesLaterPagesWithoutSilence() throws Exception {
        TutorAttachment attachment = attachment(pdfBytes(21, false));
        stubStorage(attachment);
        when(provider.chat(any())).thenReturn(AiChatResult.answered("batch"));

        VisionContextResult result = service.analyzeScannedPdf(attachment, "Review all pages");

        assertThat(result.pages()).extracting(RenderedAttachmentPage::pageNumber).contains(16, 20);
        assertThat(result.remainingPageCount()).isEqualTo(1);
        assertThat(result.warning()).isEqualTo("ATTACHMENT_PAGE_LIMIT_REACHED");
    }

    @Test
    void unavailableVisionReturnsTruthfulError() throws Exception {
        TutorAttachment attachment = attachment(pdfBytes(1, false));
        stubStorage(attachment);
        when(provider.chat(any())).thenThrow(new AiProviderException("AI_CAPABILITY_UNAVAILABLE",
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "provider detail"));

        VisionContextResult result = service.analyzeScannedPdf(attachment, "What is visible?");

        assertThat(result.status()).isEqualTo(VisionContextResult.Status.ERROR);
        assertThat(result.errorCode()).isEqualTo("AI_VISION_UNAVAILABLE");
        assertThat(result.answer()).isBlank();
    }

    private void stubStorage(TutorAttachment attachment) throws IOException {
        byte[] bytes = pdfBytesFromId(attachment.id());
        when(storage.open(attachment.storageKey())).thenAnswer(ignored -> new java.io.ByteArrayInputStream(bytes));
    }

    private TutorAttachment attachment(byte[] bytes) throws IOException {
        UUID id = UUID.randomUUID();
        pdfFixtures.put(id, bytes);
        Instant now = Instant.now();
        return new TutorAttachment(id, UUID.randomUUID(), UUID.randomUUID(), "scan.pdf", "scan.pdf", "application/pdf",
                AttachmentKind.DOCUMENT, bytes.length, "sha", "key-" + id, TutorAttachment.AttachmentStatus.STORED,
                null, null, 0, now, now, null);
    }

    private final java.util.Map<UUID, byte[]> pdfFixtures = new java.util.HashMap<>();
    private byte[] pdfBytesFromId(UUID id) { return pdfFixtures.get(id); }

    private static byte[] pdfBytes(int pages, boolean withText) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int index = 0; index < pages; index++) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                if (withText && index == 0) {
                    try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                        content.beginText();
                        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        content.newLineAtOffset(72, 700);
                        content.showText("Readable PDF text");
                        content.endText();
                    }
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
