package com.ieltsaitutor.writing;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;

@Component
public class WritingEvaluator {

    private final AiProvider aiProvider;
    private final WritingEvaluationValidator validator;

    public WritingEvaluator(AiProvider aiProvider, WritingEvaluationValidator validator) {
        this.aiProvider = aiProvider;
        this.validator = validator;
    }

    public WritingEvaluationResult evaluate(WritingEvaluationCommand command) {
        WritingTaskRubric rubric = command.rubric();
        String prompt = buildPrompt(command.taskId(), rubric.taskType(), rubric.criterionKeys());

        AiChatContext context = new AiChatContext(
                "WRITING",
                null,
                command.taskId(),
                null,
                rubric.taskType(),
                null,
                command.responseText(),
                null,
                command.taskId());

        try {
            AiChatResult result = aiProvider.chat(new AiChatCommand(prompt, context, List.of()));
            if (result == null || !"ANSWERED".equals(result.status()) || result.answer() == null) {
                return WritingEvaluationResult.failed(command.versionId(), "Đánh giá AI tạm thời không khả dụng.");
            }
            return validator.validateAndParse(command.versionId(), result.answer(), rubric);
        } catch (Exception e) {
            return WritingEvaluationResult.failed(command.versionId(), "Không thể kết nối dịch vụ đánh giá AI.");
        }
    }

    private String buildPrompt(String taskId, String taskType, List<String> criteriaKeys) {
        return """
                You are an IELTS Writing examiner. Assess the following %s response.
                You must return STRICT JSON format ONLY (no markdown fences, no extra text) with the exact keys:
                {
                  "overallBandEstimate": <number between 0.0 and 9.0 in 0.5 steps>,
                  "criteria": {
                     %s
                  },
                  "strengths": ["<strength 1>", "<strength 2>"],
                  "issues": ["<issue 1>", "<issue 2>"],
                  "suggestions": ["<suggestion 1>", "<suggestion 2>"],
                  "evidenceSpans": ["<quote from essay 1>"],
                  "priorityImprovements": ["<top priority 1>"]
                }
                Criteria keys MUST be strictly: %s.
                """.formatted(
                taskType,
                String.join(",\n     ", criteriaKeys.stream().map(k -> "\"" + k + "\": \"<feedback for " + k + ">\"").toList()),
                String.join(", ", criteriaKeys));
    }
}
