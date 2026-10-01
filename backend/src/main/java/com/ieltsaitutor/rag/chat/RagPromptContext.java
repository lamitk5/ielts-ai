package com.ieltsaitutor.rag.chat;

import java.util.List;

import com.ieltsaitutor.ai.dto.AiSource;

public record RagPromptContext(String evidence, List<AiSource> sources) {
    public RagPromptContext {
        evidence = evidence == null ? "" : evidence;
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
