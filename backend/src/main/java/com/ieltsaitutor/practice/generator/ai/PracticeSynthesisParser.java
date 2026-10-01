package com.ieltsaitutor.practice.generator.ai;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PracticeSynthesisParser {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```", Pattern.CASE_INSENSITIVE);
    private final ObjectMapper mapper;

    public PracticeSynthesisParser() {
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public RawPracticePackage parse(String rawAiResponse, String modelId, String templateVersion) {
        if (rawAiResponse == null || rawAiResponse.isBlank()) {
            throw new IllegalArgumentException("AI response cannot be null or empty");
        }

        String jsonCandidate = extractJsonString(rawAiResponse);
        try {
            JsonNode root = mapper.readTree(jsonCandidate);
            String title = root.path("title").asText("IELTS Academic Reading");

            JsonNode passageNode = root.path("passage");
            String passageTitle = passageNode.path("title").asText(title);
            List<RawPassagePayload.RawParagraphPayload> paragraphs = new ArrayList<>();
            JsonNode paragraphsArray = passageNode.path("paragraphs");
            if (paragraphsArray.isArray()) {
                int idx = 1;
                for (JsonNode pNode : paragraphsArray) {
                    String pId = pNode.path("id").asText("p" + idx);
                    String text = pNode.path("text").asText("");
                    paragraphs.add(new RawPassagePayload.RawParagraphPayload(pId, text));
                    idx++;
                }
            }

            List<RawQuestionPayload> questions = new ArrayList<>();
            JsonNode questionsArray = root.path("questions");
            if (questionsArray.isArray()) {
                int qIdx = 1;
                for (JsonNode qNode : questionsArray) {
                    String qId = qNode.path("id").asText("q" + qIdx);
                    String taskType = qNode.path("taskType").asText("MULTIPLE_CHOICE");
                    String prompt = qNode.path("prompt").asText("");
                    List<String> options = new ArrayList<>();
                    JsonNode optionsArray = qNode.path("options");
                    if (optionsArray.isArray()) {
                        for (JsonNode opt : optionsArray) {
                            options.add(opt.asText(""));
                        }
                    }
                    String answerKey = qNode.path("answerKey").asText("");
                    String evidenceSpan = qNode.path("evidenceSpan").asText("");
                    String explanation = qNode.path("explanation").asText("");

                    questions.add(new RawQuestionPayload(qId, taskType, prompt, options, answerKey, evidenceSpan, explanation));
                    qIdx++;
                }
            }

            return new RawPracticePackage(
                    title,
                    new RawPassagePayload(passageTitle, paragraphs),
                    questions,
                    modelId != null ? modelId : "ai-provider",
                    templateVersion != null ? templateVersion : "v1.0",
                    Instant.now()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse synthesized practice JSON from AI provider response", e);
        }
    }

    private String extractJsonString(String text) {
        String trimmed = text.trim();
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1).trim();
        }
        return trimmed;
    }
}
