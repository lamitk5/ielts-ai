package com.ieltsaitutor.rag.admin.dto;

import java.util.UUID;

public record RagActionResponse(String action, UUID documentId, String rightsStatus, String indexStatus, boolean active) {}
