package com.ieltsaitutor.rag.chat;

import java.util.List;
import java.util.Set;

import com.ieltsaitutor.ai.dto.AiSource;

public interface GroundingValidator {
    List<AiSource> validate(List<AiSource> proposed, Set<String> retrievedSourceIds);
}
