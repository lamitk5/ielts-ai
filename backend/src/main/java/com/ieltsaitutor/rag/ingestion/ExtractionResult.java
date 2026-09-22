package com.ieltsaitutor.rag.ingestion;

import com.ieltsaitutor.rag.domain.ExtractionStatus;

public record ExtractionResult(ExtractionStatus status, ExtractedDocument document, DocumentPreview preview,
        String errorCode, String errorMessage) {
    public static ExtractionResult ready(ExtractedDocument document) {
        String previewText = document.text().substring(0, Math.min(4000, document.text().length()));
        return new ExtractionResult(ExtractionStatus.READY_FOR_REVIEW, document,
                new DocumentPreview(previewText, document.text().length()), null, null);
    }

    public static ExtractionResult needsOcr(String message) {
        return new ExtractionResult(ExtractionStatus.NEEDS_OCR, null, null, "RAG_NEEDS_OCR", message);
    }

    public static ExtractionResult failed(String message) {
        return new ExtractionResult(ExtractionStatus.FAILED, null, null, "RAG_EXTRACTION_FAILED", message);
    }
}
