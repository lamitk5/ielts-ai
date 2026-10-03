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
import com.ieltsaitutor.learning.intelligence.LearningEvidencePipeline;

@Service
public class WritingAssessmentService {
    private static final String DISCLAIMER = "Band ước lượng — không phải điểm thi chính thức.";
    private final AiProvider provider;
    private final WritingRepository repository;
    private final ObjectMapper objectMapper;
    private final LearningEvidencePipeline evidence;

    public WritingAssessmentService(AiProvider provider, WritingRepository repository) {
        this(provider, repository, new ObjectMapper(), null);
    }

    public WritingAssessmentService(AiProvider provider, WritingRepository repository, ObjectMapper objectMapper) {
        this(provider, repository, objectMapper, null);
    }

    @Autowired
    public WritingAssessmentService(AiProvider provider, WritingRepository repository, LearningEvidencePipeline evidence) {
        this(provider, repository, new ObjectMapper(), evidence);
    }

    public WritingAssessmentService(AiProvider provider, WritingRepository repository, ObjectMapper objectMapper,
            LearningEvidencePipeline evidence) {
        this.provider = provider; this.repository = repository; this.objectMapper = objectMapper; this.evidence = evidence;
    }

    public WritingAssessment assess(UUID userId, String taskId, String responseText) {
        return evaluate(userId, taskId, responseText, true);
    }

    public WritingAttempt startAttempt(UUID userId, String taskId) {
        if (taskType(taskId) == null) throw new IllegalArgumentException("Writing task không hợp lệ");
        return repository.start(userId, taskId);
    }

    public WritingAttempt saveAttemptDraft(UUID userId, UUID attemptId, String responseText) {
        WritingAttempt attempt = owned(userId, attemptId);
        if (!"IN_PROGRESS".equals(attempt.status())) throw new IllegalStateException("Writing attempt đã được nộp");
        return repository.saveDraft(attempt.withDraft(responseText));
    }

    public WritingAttempt getAttempt(UUID userId, UUID attemptId) {
        return owned(userId, attemptId);
    }

    public WritingAttempt submitAttempt(UUID userId, UUID attemptId, String responseText) {
        WritingAttempt attempt = owned(userId, attemptId);
        if (!"IN_PROGRESS".equals(attempt.status())) return attempt;
        WritingAssessment assessment = evaluate(userId, attempt.taskId(), responseText, false);
        emit(userId, attempt.taskId(), assessment.wordCount());
        return repository.complete(attempt.withDraft(responseText), assessment);
    }

    private WritingAssessment evaluate(UUID userId, String taskId, String responseText, boolean persist) {
        String taskType = taskType(taskId);
        if (taskType == null) return unavailable(userId, taskId);
        try {
            AiChatResult result = provider.chat(new AiChatCommand(
                    assessmentPrompt(responseText),
                    new AiChatContext("WRITING", null, taskId, null, taskType, null, responseText, null, taskId),
                    List.of()));
            if (!"ANSWERED".equals(result.status())) return persistIfNeeded(userId, taskId, unavailable(userId, taskId, responseText), persist);
            WritingAssessment assessment = parse(userId, taskId, responseText, result.answer());
            return persistIfNeeded(userId, taskId, assessment, persist);
        } catch (RuntimeException exception) {
            return persistIfNeeded(userId, taskId, unavailable(userId, taskId, responseText), persist);
        }
    }

    private WritingAttempt owned(UUID userId, UUID attemptId) {
        return repository.find(attemptId, userId).filter(attempt -> attempt.userId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Writing attempt không tồn tại"));
    }

    private String taskType(String taskId) {
        if (taskId != null && taskId.startsWith("task-1")) return "TASK_1";
        if (taskId != null && taskId.startsWith("task-2")) return "TASK_2";
        return null;
    }

    private WritingAssessment parse(UUID userId, String taskId, String responseText, String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode estimate = root.get("overallBandEstimate");
            if (estimate == null || !estimate.isNumber() || estimate.doubleValue() < 0 || estimate.doubleValue() > 9) {
                throw new IllegalArgumentException("Invalid estimate");
            }
            Map<String, String> criteria = new LinkedHashMap<>();
            root.path("criteria").fields().forEachRemaining(entry -> criteria.put(entry.getKey(), entry.getValue().asText()));
            return new WritingAssessment("ANSWERED", userId, taskId, estimate.doubleValue(), criteria,
                    strings(root.get("strengths")), strings(root.get("issues")), strings(root.get("suggestions")),
                    List.of(), "NOT_ENABLED", false, DISCLAIMER, java.time.Instant.now(), responseText, wordCount(responseText));
        } catch (Exception exception) {
            return unavailable(userId, taskId, responseText);
        }
    }

    private String assessmentPrompt(String responseText) {
        return "Assess this IELTS writing response. Return JSON only with overallBandEstimate, criteria, strengths, issues, suggestions."
                + "\n\nWriting response:\n" + (responseText == null ? "" : responseText);
    }

    private int wordCount(String responseText) { return responseText == null || responseText.isBlank() ? 0 : responseText.trim().split("\\s+").length; }

    private List<String> strings(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) node.forEach(value -> values.add(value.asText()));
        return List.copyOf(values);
    }

    private WritingAssessment persistUnavailable(UUID userId, String taskId) {
        return persistIfNeeded(userId, taskId, unavailable(userId, taskId), true);
    }

    private WritingAssessment persistIfNeeded(UUID userId, String taskId, WritingAssessment assessment, boolean persist) {
        if (persist) {
            repository.save(assessment);
            emit(userId, taskId, assessment.wordCount());
        }
        return assessment;
    }

    private void emit(UUID userId, String taskId, int wordCount) {
        if (evidence == null) return;
        try { evidence.writingSubmitted(userId, taskId, wordCount, java.time.Instant.now()); }
        catch (RuntimeException ignored) { /* adaptive refresh must not break assessment persistence */ }
    }

    private WritingAssessment unavailable(UUID userId, String taskId) {
        return WritingAssessment.unavailable(userId, taskId);
    }

    private WritingAssessment unavailable(UUID userId, String taskId, String responseText) {
        return new WritingAssessment("UNAVAILABLE", userId, taskId, null, Map.of(), List.of(), List.of(), List.of(),
                List.of(), "NOT_ENABLED", false, "Chưa có đánh giá AI khả dụng cho bài viết này.",
                java.time.Instant.now(), responseText, wordCount(responseText));
    }
}
