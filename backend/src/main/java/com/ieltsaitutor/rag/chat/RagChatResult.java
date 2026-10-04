package com.ieltsaitutor.rag.chat;

import java.util.List;

import com.ieltsaitutor.ai.dto.AiGrounding;
import com.ieltsaitutor.ai.dto.AiSource;

public record RagChatResult(String status, String answer, List<AiSource> sources, AiGrounding grounding) {
    public RagChatResult {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
