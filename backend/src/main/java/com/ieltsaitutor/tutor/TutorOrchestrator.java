package com.ieltsaitutor.tutor;

import com.ieltsaitutor.ai.dto.AiChatResponse;
import com.ieltsaitutor.ai.dto.ChatHistoryItem;
import com.ieltsaitutor.ai.dto.AiGrounding;
import com.ieltsaitutor.ai.dto.AiSource;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.ai.exception.AiProviderException;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.rag.chat.RagChatResult;
import com.ieltsaitutor.rag.chat.RagChatService;
import com.ieltsaitutor.tutor.context.TutorContextRequest;
import com.ieltsaitutor.tutor.context.TutorContextService;
import com.ieltsaitutor.tutor.context.TutorLearningContext;
import com.ieltsaitutor.tutor.intent.TutorIntent;
import com.ieltsaitutor.tutor.intent.TutorIntentRoute;
import com.ieltsaitutor.tutor.intent.TutorIntentRouter;
import com.ieltsaitutor.tutor.tool.DeterministicTutorTools;
import com.ieltsaitutor.tutor.tool.TutorToolResult;
import com.ieltsaitutor.tutor.security.TutorRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ieltsaitutor.tutor.memory.AiConversation;
import com.ieltsaitutor.tutor.memory.AiMessage;
import com.ieltsaitutor.tutor.memory.AiMessageRole;
import com.ieltsaitutor.tutor.memory.ConversationService;

@Service
public class TutorOrchestrator {
    private static final String INSUFFICIENT = "Chưa đủ ngữ cảnh để trả lời chắc chắn. Hãy mở đúng bài hoặc cung cấp thêm thông tin liên quan.";
    private final AiProvider provider;
    private final RagChatService rag;
    private final TutorContextService contexts;
    private final TutorIntentRouter intents;
    private final DeterministicTutorTools tools;
    private final TutorRateLimiter rateLimiter;
    private final ConversationService conversations;

    @Autowired
    public TutorOrchestrator(AiProvider provider, RagChatService rag, TutorContextService contexts,
            TutorIntentRouter intents, DeterministicTutorTools tools, TutorRateLimiter rateLimiter,
            ConversationService conversations) {
        this.provider = provider;
        this.rag = rag;
        this.contexts = contexts;
        this.intents = intents;
        this.tools = tools;
        this.rateLimiter = rateLimiter;
        this.conversations = conversations;
    }

    public TutorOrchestrator(AiProvider provider, RagChatService rag, TutorContextService contexts,
            TutorIntentRouter intents, DeterministicTutorTools tools, TutorRateLimiter rateLimiter) {
        this(provider, rag, contexts, intents, tools, rateLimiter, null);
    }

    public TutorOrchestrator(AiProvider provider, RagChatService rag, TutorContextService contexts,
            TutorIntentRouter intents, DeterministicTutorTools tools) {
        this(provider, rag, contexts, intents, tools, new TutorRateLimiter(10, 30, java.time.Duration.ofMinutes(1)));
    }

    public TutorOrchestrator(AiProvider provider, RagChatService rag, TutorContextService contexts,
            TutorIntentRouter intents, DeterministicTutorTools tools, ConversationService conversations) {
        this(provider, rag, contexts, intents, tools, new TutorRateLimiter(10, 30, java.time.Duration.ofMinutes(1)), conversations);
    }

    public AiChatResponse handle(AuthPrincipal principal, com.ieltsaitutor.ai.dto.AiChatRequest request) {
        String requestId = UUID.randomUUID().toString();
        if (conversationIsNotOwned(principal, request)) {
            return response("INVALID_CONVERSATION", "Không thể truy cập cuộc hội thoại này.", List.of(),
                    new AiGrounding("NOT_ENABLED", false), requestId);
        }
        TutorLearningContext context = contexts.resolve(principal, contextRequest(request));
        TutorIntentRoute route = intents.route(request, context);
        if (route.intent() == TutorIntent.APP_DATA || route.intent() == TutorIntent.PROGRESS_HISTORY) {
            TutorToolResult result = tools.execute(route.intent(), context);
            return persist(principal, request, response(result.status(), result.answer(), List.of(),
                    new AiGrounding("NOT_ENABLED", false), requestId), context);
        }
        if (route.intent() == TutorIntent.OUT_OF_SCOPE) {
            return persist(principal, request, response("OUT_OF_SCOPE",
                    "Mình tập trung vào tiếng Anh và IELTS. Nếu bạn muốn, mình có thể giúp bạn luyện từ vựng hoặc Speaking về chủ đề này.",
                    List.of(), new AiGrounding("NOT_ENABLED", false), requestId), context);
        }
        if (!route.externalAiAllowed()) {
            return persist(principal, request, response("INSUFFICIENT_CONTEXT", INSUFFICIENT, List.of(),
                    new AiGrounding("INSUFFICIENT_CONTEXT", false), requestId), context);
        }
        if (!rateLimiter.allow(principal).allowed()) {
            throw new AiProviderException("AI_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS,
                    "Trợ giảng AI đang nhận nhiều yêu cầu. Hãy thử lại sau một chút.");
        }
        AiChatCommand command = new AiChatCommand(request.message().trim(), request.context(), boundedHistory(principal, request),
                requestId, compactContext(context));
        if (route.ragAllowed()) {
            RagChatResult result = rag.chat(command);
            return persist(principal, request, response(result.status(), result.answer(), result.sources(), result.grounding(), requestId), context);
        }
        AiChatResult result = provider.chat(command);
        return persist(principal, request, response(result.status(), result.answer(), List.of(),
                new AiGrounding("NOT_ENABLED", false), requestId), context);
    }

    private boolean conversationIsNotOwned(AuthPrincipal principal, com.ieltsaitutor.ai.dto.AiChatRequest request) {
        return conversations != null && principal != null && request.conversationId() != null
                && conversations.findOwned(principal.userId(), request.conversationId()).isEmpty();
    }

    private AiChatResponse persist(AuthPrincipal principal, com.ieltsaitutor.ai.dto.AiChatRequest request,
            AiChatResponse result, TutorLearningContext context) {
        if (conversations == null || principal == null) return result;
        AiConversation conversation;
        if (request.conversationId() == null) {
            String skill = request.context() == null ? "general" : request.context().normalizedSkill().toLowerCase();
            conversation = conversations.create(principal.userId(), skill,
                    request.context() == null ? null : request.context().exerciseId(),
                    context == null ? null : parseUuid(request.context() == null ? null : request.context().attemptId()),
                    request.context() == null ? null : request.context().questionId(),
                    bounded(request.message(), 120));
        } else {
            conversation = conversations.findOwned(principal.userId(), request.conversationId()).orElse(null);
        }
        if (conversation == null) return result;
        int next = conversations.messages(principal.userId(), conversation.id()).size() + 1;
        conversations.appendMessage(principal.userId(), conversation.id(), new AiMessage(UUID.randomUUID(), conversation.id(),
                next, AiMessageRole.USER, bounded(request.message(), 4_000), "USER_MESSAGE", null, List.of(), Map.of(), Instant.now()));
        conversations.appendMessage(principal.userId(), conversation.id(), new AiMessage(UUID.randomUUID(), conversation.id(),
                next + 1, AiMessageRole.ASSISTANT, bounded(result.answer(), 12_000), result.status(),
                result.grounding() == null ? null : result.grounding().status(), result.sources(), Map.of(), result.timestamp()));
        return new AiChatResponse(result.status(), result.answer(), result.sources(), result.grounding(), result.references(),
                new AiChatResponse.Meta(result.meta() == null ? null : result.meta().requestId(), conversation.id()), result.timestamp());
    }

    private String bounded(String value, int max) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private TutorContextRequest contextRequest(com.ieltsaitutor.ai.dto.AiChatRequest request) {
        var source = request.context();
        if (source == null) return new TutorContextRequest("general", null, null, null, null, null);
        UUID attempt = parseUuid(source.attemptId());
        String skill = source.normalizedSkill().toLowerCase();
        String setId = source.exerciseId();
        String taskId = "writing".equals(skill) ? source.exerciseId() : null;
        String promptId = "speaking".equals(skill)
                ? (source.promptId() == null ? source.exerciseId() : source.promptId()) : null;
        return new TutorContextRequest(skill, setId, source.questionId(), attempt, taskId, promptId);
    }

    private UUID parseUuid(String value) {
        try { return value == null ? null : UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private List<ChatHistoryItem> boundedHistory(AuthPrincipal principal, com.ieltsaitutor.ai.dto.AiChatRequest request) {
        if (conversations != null && principal != null && request.conversationId() != null) {
            List<ChatHistoryItem> stored = conversations.messages(principal.userId(), request.conversationId()).stream()
                    .map(message -> new ChatHistoryItem(message.role() == AiMessageRole.USER ? "USER" : "ASSISTANT", message.content()))
                    .toList();
            return stored.stream().skip(Math.max(0, stored.size() - 8)).toList();
        }
        List<ChatHistoryItem> history = request.history();
        return history.stream().skip(Math.max(0, history.size() - 8)).toList();
    }

    private String compactContext(TutorLearningContext context) {
        if (context == null || !context.available()) return null;
        StringBuilder evidence = new StringBuilder();
        append(evidence, "skill", context.skill());
        append(evidence, "question", context.questionPrompt());
        append(evidence, "selected_answer", context.selectedAnswer());
        append(evidence, "correct_answer", context.correctAnswer());
        append(evidence, "explanation", context.explanation());
        append(evidence, "writing_draft", context.writingText());
        append(evidence, "speaking_transcript", context.speakingTranscript());
        return evidence.length() == 0 ? null : evidence.toString();
    }

    private void append(StringBuilder builder, String key, String value) {
        if (value != null && !value.isBlank()) builder.append(key).append(": ").append(value).append('\n');
    }

    private AiChatResponse response(String status, String answer, List<AiSource> sources, AiGrounding grounding,
            String requestId) {
        return new AiChatResponse(status, answer, sources, grounding, new AiChatResponse.Meta(requestId), Instant.now());
    }
}
