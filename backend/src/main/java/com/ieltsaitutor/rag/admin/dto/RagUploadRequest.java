package com.ieltsaitutor.rag.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.ieltsaitutor.rag.domain.Skill;

public record RagUploadRequest(@NotBlank String title, String author, String organization, String sourceType,
        @NotBlank String language, @NotNull Skill skill, String version, String rightsStatus, String rightsNote) {}
