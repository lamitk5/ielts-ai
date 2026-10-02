package com.ieltsaitutor.writing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class WritingEvaluationValidator {

    private final ObjectMapper objectMapper;

    public WritingEvaluationValidator() {
        this(new ObjectMapper());
    }

    public WritingEvaluationValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public WritingEvaluationResult validateAndParse(String rawJson, WritingTaskRubric rubric) {
        return validateAndParse(UUID.randomUUID(), rawJson, rubric);
    }

    public WritingEvaluationResult validateAndParse(UUID versionId, String rawJson, WritingTaskRubric rubric) {
        if (rawJson == null || rawJson.isBlank() || rubric == null) {
            return WritingEvaluationResult.malformed(versionId);
        }
        try {
            // If json is wrapped in markdown ```json ... ```, extract content
            String cleanJson = rawJson.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            JsonNode root = objectMapper.readTree(cleanJson);
            JsonNode estimateNode = root.get("overallBandEstimate");
            if (estimateNode == null || !estimateNode.isNumber()) {
                return WritingEvaluationResult.malformed(versionId);
            }
            double band = estimateNode.doubleValue();
            if (band < 0.0 || band > 9.0) {
                return WritingEvaluationResult.malformed(versionId);
            }

            JsonNode criteriaNode = root.get("criteria");
            if (criteriaNode == null || !criteriaNode.isObject()) {
                return WritingEvaluationResult.malformed(versionId);
            }

            Map<String, String> criteria = new LinkedHashMap<>();
            criteriaNode.fields().forEachRemaining(entry -> criteria.put(entry.getKey(), entry.getValue().asText()));

            if (!rubric.isValidCriteria(criteria)) {
                return WritingEvaluationResult.malformed(versionId);
            }

            List<String> strengths = extractStringList(root.get("strengths"));
            List<String> issues = extractStringList(root.get("issues"));
            List<String> suggestions = extractStringList(root.get("suggestions"));
            List<String> evidenceSpans = extractStringList(root.get("evidenceSpans"));
            List<String> priorityImprovements = extractStringList(root.get("priorityImprovements"));

            String grounding = root.has("groundingStatus") ? root.get("groundingStatus").asText() : "NOT_ENABLED";

            return new WritingEvaluationResult(
                    UUID.randomUUID(),
                    versionId,
                    1,
                    band,
                    criteria,
                    strengths,
                    issues,
                    suggestions,
                    evidenceSpans,
                    priorityImprovements,
                    grounding,
                    WritingEvaluationResult.STANDARD_DISCLAIMER,
                    "GRADED",
                    Instant.now());
        } catch (Exception e) {
            return WritingEvaluationResult.malformed(versionId);
        }
    }

    private List<String> extractStringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                if (item != null && !item.asText().isBlank()) {
                    result.add(item.asText());
                }
            });
        }
        return List.copyOf(result);
    }
}
