package com.ieltsaitutor.rag.ingestion;

import java.util.Map;

public record ExtractedSection(String title, String content, Integer pageNumber, Map<String, Object> metadata) {}
