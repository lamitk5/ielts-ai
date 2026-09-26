package com.ieltsaitutor.practice.generator.dto;

import com.ieltsaitutor.rag.domain.RightsStatus;

public record RegisterSourceRequest(
        String title,
        String sourceType,
        String author,
        RightsStatus rightsStatus,
        String licenseNote,
        String content) {

    public RegisterSourceRequest {
        if (sourceType == null || sourceType.isBlank()) sourceType = "PASTED_TEXT";
        if (rightsStatus == null) rightsStatus = RightsStatus.PENDING_REVIEW;
        if (licenseNote == null) licenseNote = "";
    }
}
