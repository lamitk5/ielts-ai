package com.ieltsaitutor.ai.dto;

public record AiSource(String sourceId, String title, String section, String version, Integer page, String chunkId) {
    public AiSource(String sourceId, String title, String section) {
        this(sourceId, title, section, null, null, null);
    }
}
