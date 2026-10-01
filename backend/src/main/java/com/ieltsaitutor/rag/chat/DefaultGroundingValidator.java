package com.ieltsaitutor.rag.chat;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.ieltsaitutor.ai.dto.AiSource;

@Service
public class DefaultGroundingValidator implements GroundingValidator {
    @Override
    public List<AiSource> validate(List<AiSource> proposed, Set<String> retrievedSourceIds) {
        if (proposed == null || retrievedSourceIds == null) return List.of();
        return proposed.stream().filter(source -> source != null && source.sourceId() != null
                && retrievedSourceIds.contains(source.sourceId())).toList();
    }
}
