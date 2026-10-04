package com.ieltsaitutor.practice.generator.dto;

import java.time.Instant;
import java.util.UUID;

import com.ieltsaitutor.rag.domain.RightsStatus;

public record RegisterSourceResponse(
        UUID id,
        String title,
        RightsStatus rightsStatus,
        int wordCount,
        String checksum,
        Instant createdAt) {
}
