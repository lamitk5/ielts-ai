package com.ieltsaitutor.rag.ingestion;

import java.util.UUID;

public interface DocumentIngestionService {
    UploadReceipt upload(UploadCommand command);
    DocumentPreview extractPreview(UUID documentId, UUID versionId);
    IndexReceipt index(UUID documentId, UUID versionId, IndexMode mode);
}
