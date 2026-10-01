package com.ieltsaitutor.rag.ingestion;

import java.util.List;
import java.util.Map;

public record ExtractedDocument(String text, List<ExtractedSection> sections, Map<String, Object> metadata) {}
