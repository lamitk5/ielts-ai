package com.ieltsaitutor.ai.attachment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

@Service
public class PdfPageRenderer {
    public static final int MAX_RENDERED_PAGES = 20;
    private static final float DPI = 120f;

    public List<RenderedAttachmentPage> renderRelevantPages(Path pdf, PageRange range) {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            int first = Math.min(range.firstPage(), document.getNumberOfPages());
            int last = Math.min(range.lastPage(), document.getNumberOfPages());
            if (first > last || document.getNumberOfPages() == 0) return List.of();
            last = Math.min(last, first + MAX_RENDERED_PAGES - 1);
            PDFRenderer renderer = new PDFRenderer(document);
            List<RenderedAttachmentPage> pages = new ArrayList<>();
            for (int page = first; page <= last; page++) {
                ByteArrayOutputStream image = new ByteArrayOutputStream();
                ImageIO.write(renderer.renderImageWithDPI(page - 1, DPI, ImageType.RGB), "jpg", image);
                pages.add(new RenderedAttachmentPage(null, page, "image/jpeg", image.toByteArray()));
            }
            return List.copyOf(pages);
        } catch (IOException exception) {
            throw new IllegalStateException("ATTACHMENT_PDF_RENDER_FAILED", exception);
        }
    }
}
