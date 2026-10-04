package com.ieltsaitutor.rag.ingestion;

import java.io.InputStream;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentStorageService {
    StoredDocument store(MultipartFile file, UUID documentId, UUID versionId);
    InputStream open(StoredDocument storedDocument);
}
