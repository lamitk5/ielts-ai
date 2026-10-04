package com.ieltsaitutor.rag.ingestion;

import org.springframework.web.multipart.MultipartFile;

import com.ieltsaitutor.rag.domain.Skill;

public record UploadCommand(String title, String sourceType, String author, String organization, String language,
        Skill skill, MultipartFile file) {
    public UploadCommand {
        if (title == null || title.isBlank() || language == null || language.isBlank() || skill == null || file == null) {
            throw new IllegalArgumentException("Document title, language, skill, and file are required");
        }
    }
}
