package com.ieltsaitutor.rag.chat;

import java.util.UUID;

public record SourceProvenance(String sourceId, String title, String version, Integer page, String section,
        UUID chunkId) {}
