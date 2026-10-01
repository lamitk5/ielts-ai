package com.ieltsaitutor.rag.ingestion;

import java.nio.file.Path;

public record StoredDocument(Path absolutePath, String relativePath, String originalFilename, String mimeType,
        long fileSizeBytes, String checksum) {}
