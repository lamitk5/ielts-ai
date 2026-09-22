package com.ieltsaitutor.rag.cli;

import java.nio.file.Path;

import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.domain.Skill;

public record RagManifestEntry(Path file, String title, String author, String organization, String version,
        String language, Skill skill, RightsStatus rightsStatus, String rightsNote, String sourceType) {}
