package com.ieltsaitutor.ai.service;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.ai.dto.AiChatResponse;
import com.ieltsaitutor.ai.dto.AiSource;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.rag.chat.RagChatResult;
import com.ieltsaitutor.rag.chat.RagChatService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final RagChatService ragChatService;

    public AiChatService(AiProvider provider) { this(provider, null); }

    @Autowired
    public AiChatService(AiProvider provider, RagChatService ragChatService) {
        this.provider = provider;
        this.ragChatService = ragChatService;
    }

    public AiChatResponse chat(AiChatRequest request) {
        String requestId = UUID.randomUUID().toString();
        String message = request.message().trim();
        AiChatContext context = request.context() == null
                ? new AiChatContext("GENERAL", null, null, null, null, null, null)
                : request.context();
        List<com.ieltsaitutor.ai.dto.ChatHistoryItem> history = request.history().stream()
                .skip(Math.max(0, request.history().size() - MAX_HISTORY_MESSAGES))
                .toList();

        boolean insufficientContext = hasNoRelevantContext(message, context);
        AiChatCommand command = new AiChatCommand(message, context, history, requestId);
        RagChatResult ragResult = insufficientContext
                ? null
                : ragChatService == null ? null : ragChatService.chat(command);
        AiChatResult result = insufficientContext
                ? AiChatResult.insufficientContext(INSUFFICIENT_CONTEXT_ANSWER)
                : ragResult == null ? provider.chat(command) : new AiChatResult(ragResult.status(), ragResult.answer());

        return new AiChatResponse(
                result.status(),
                result.answer(),
                ragResult == null ? List.<AiSource>of() : ragResult.sources(),
                ragResult == null ? new com.ieltsaitutor.ai.dto.AiGrounding("NOT_ENABLED", false) : ragResult.grounding(),
                new AiChatResponse.Meta(requestId),
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
