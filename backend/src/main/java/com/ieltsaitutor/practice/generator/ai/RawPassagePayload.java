package com.ieltsaitutor.practice.generator.ai;

import java.util.List;

public record RawPassagePayload(
        String title,
        List<RawParagraphPayload> paragraphs) {

    public RawPassagePayload {
        if (title == null || title.isBlank()) title = "Reading Passage";
        if (paragraphs == null) paragraphs = List.of();
    }

    public record RawParagraphPayload(String id, String text) {}
}
