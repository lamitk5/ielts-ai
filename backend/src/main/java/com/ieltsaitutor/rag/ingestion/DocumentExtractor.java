package com.ieltsaitutor.rag.ingestion;

public interface DocumentExtractor {
    ExtractionResult extract(StoredDocument storedDocument);
}
