package com.ieltsaitutor.writing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;

@Service
public class WritingAssessmentService {
    private static final String DISCLAIMER = "Band ước lượng — không phải điểm thi chính thức.";
    private final AiProvider provider;
    private final WritingRepository repository;
    private final ObjectMapper objectMapper;

    @Autowired
    public WritingAssessmentService(AiProvider provider, WritingRepository repository) {
        this(provider, repository, new ObjectMapper());
    }

    public WritingAssessmentService(AiProvider provider, WritingRepository repository, ObjectMapper objectMapper) {
        this.provider = provider;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public WritingAssessment assess(UUID userId, String taskId, String responseText) {
        try {
            AiChatResult result = provider.chat(new AiChatCommand(
                    "Assess this IELTS writing response. Return JSON only with overallBandEstimate, criteria, strengths, issues, suggestions.",
                    new AiChatContext("WRITING", null, taskId, null, taskId.startsWith("task-1") ? "TASK_1" : "TASK_2", null, responseText),
                    List.of()));
            if (!"ANSWERED".equals(result.status())) return persistUnavailable(userId, taskId);
            WritingAssessment assessment = parse(userId, taskId, responseText, result.answer());
            repository.save(assessment);
            return assessment;
        } catch (RuntimeException exception) {
            return persistUnavailable(userId, taskId);
        }
    }

    private WritingAssessment parse(UUID userId, String taskId, String responseText, String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode estimate = root.get("overallBandEstimate");
            if (estimate == null || !estimate.isNumber() || estimate.doubleValue() < 0 || estimate.doubleValue() > 9) throw new IllegalArgumentException("Invalid estimate");
            Map<String, String> criteria = new LinkedHashMap<>();
            root.path("criteria").fields().forEachRemaining(entry -> criteria.put(entry.getKey(), entry.getValue().asText()));
            return new WritingAssessment("ANSWERED", userId, taskId, estimate.doubleValue(), criteria,
                    strings(root.get("strengths")), strings(root.get("issues")), strings(root.get("suggestions")),
                    List.of(), "NOT_ENABLED", false, DISCLAIMER, java.time.Instant.now(), responseText, wordCount(responseText));
        } catch (Exception exception) {
            return unavailable(userId, taskId);
        }
    }

    private int wordCount(String responseText) { return responseText == null || responseText.isBlank() ? 0 : responseText.trim().split("\\s+").length; }

    private List<String> strings(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) node.forEach(value -> values.add(value.asText()));
        return List.copyOf(values);
    }

    private WritingAssessment persistUnavailable(UUID userId, String taskId) {
        WritingAssessment assessment = unavailable(userId, taskId);
        repository.save(assessment);
        return assessment;
    }

    private WritingAssessment unavailable(UUID userId, String taskId) {
        return WritingAssessment.unavailable(userId, taskId);
    }
}
