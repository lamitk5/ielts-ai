package com.ieltsaitutor.ai.service;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.ai.dto.AiChatResponse;
import com.ieltsaitutor.ai.dto.AiSource;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AiChatService {
    public static final int MAX_HISTORY_MESSAGES = 8;
    public static final String INSUFFICIENT_CONTEXT_ANSWER =
            "Chưa đủ thông tin để trả lời chắc chắn. Hãy cung cấp câu hỏi, đoạn văn hoặc bài làm liên quan.";
    private static final Pattern CONTEXT_DEPENDENT_QUESTION = Pattern.compile(
            "(?iu)(câu\\s*\\d+|question\\s*\\d+).*(false|true|not given|đáp án|answer)|"
                    + "(đoạn văn|bài tôi|bài đang làm|passage|exercise).*(sai|đúng|false|true|answer|đáp án)");

    private final AiProvider provider;

    public AiChatService(AiProvider provider) {
        this.provider = provider;
    }

    public AiChatResponse chat(AiChatRequest request) {
        String message = request.message().trim();
        AiChatContext context = request.context() == null
                ? new AiChatContext("GENERAL", null, null, null, null, null, null)
                : request.context();
        List<com.ieltsaitutor.ai.dto.ChatHistoryItem> history = request.history().stream()
                .skip(Math.max(0, request.history().size() - MAX_HISTORY_MESSAGES))
                .toList();

        AiChatResult result = hasNoRelevantContext(message, context)
                ? AiChatResult.insufficientContext(INSUFFICIENT_CONTEXT_ANSWER)
                : provider.chat(new AiChatCommand(message, context, history));

        return new AiChatResponse(
                result.status(),
                result.answer(),
                List.<AiSource>of(),
                new com.ieltsaitutor.ai.dto.AiGrounding("NOT_ENABLED", false),
                new AiChatResponse.Meta(UUID.randomUUID().toString()),
                Instant.now());
    }

    private boolean hasNoRelevantContext(String message, AiChatContext context) {
        boolean hasExerciseEvidence = context != null && (
                hasText(context.selectedText()) || hasText(context.questionId()) || hasText(context.exerciseId()));
        return !hasExerciseEvidence && CONTEXT_DEPENDENT_QUESTION.matcher(message).find();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
