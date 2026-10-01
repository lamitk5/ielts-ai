package com.ieltsaitutor.rag.ingestion;

import java.util.UUID;

import com.ieltsaitutor.rag.domain.IndexStatus;
import com.ieltsaitutor.rag.domain.RightsStatus;

public record UploadReceipt(UUID documentId, UUID versionId, UUID jobId, RightsStatus rightsStatus,
        IndexStatus indexStatus, boolean active, boolean duplicate) {}
