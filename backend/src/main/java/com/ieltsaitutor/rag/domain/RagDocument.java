package com.ieltsaitutor.rag.domain;

import java.time.Instant;
import java.util.UUID;

public record RagDocument(UUID id, String title, String sourceType, String author, String organization,
        String language, Skill skill, RightsStatus rightsStatus, String rightsNote, boolean active,
        UUID currentVersionId, Instant createdAt, Instant updatedAt) {}
