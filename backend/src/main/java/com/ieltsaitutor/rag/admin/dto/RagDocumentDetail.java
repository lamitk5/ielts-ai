package com.ieltsaitutor.rag.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record RagDocumentDetail(UUID id, String title, String skill, String rightsStatus, String indexStatus,
        boolean active, RagPreviewResponse preview, Instant updatedAt) {}
