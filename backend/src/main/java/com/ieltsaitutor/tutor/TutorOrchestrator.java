package com.ieltsaitutor.tutor;

import com.ieltsaitutor.ai.dto.AiChatResponse;
import com.ieltsaitutor.ai.dto.AiGrounding;
import com.ieltsaitutor.ai.dto.AiSource;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
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
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TutorOrchestrator {
    private static final String INSUFFICIENT = "Chưa đủ ngữ cảnh để trả lời chắc chắn. Hãy mở đúng bài hoặc cung cấp thêm thông tin liên quan.";
    private final AiProvider provider;
    private final RagChatService rag;
    private final TutorContextService contexts;
    private final TutorIntentRouter intents;
    private final DeterministicTutorTools tools;

    public TutorOrchestrator(AiProvider provider, RagChatService rag, TutorContextService contexts,
            TutorIntentRouter intents, DeterministicTutorTools tools) {
        this.provider = provider;
        this.rag = rag;
        this.contexts = contexts;
        this.intents = intents;
        this.tools = tools;
    }

    public AiChatResponse handle(AuthPrincipal principal, com.ieltsaitutor.ai.dto.AiChatRequest request) {
        String requestId = UUID.randomUUID().toString();
        TutorLearningContext context = contexts.resolve(principal, contextRequest(request));
        TutorIntentRoute route = intents.route(request, context);
        if (route.intent() == TutorIntent.APP_DATA || route.intent() == TutorIntent.PROGRESS_HISTORY) {
            TutorToolResult result = tools.execute(route.intent(), context);
            return response(result.status(), result.answer(), List.of(), new AiGrounding("NOT_ENABLED", false), requestId);
        }
        if (!route.externalAiAllowed()) {
            return response("INSUFFICIENT_CONTEXT", INSUFFICIENT, List.of(),
                    new AiGrounding("INSUFFICIENT_CONTEXT", false), requestId);
        }
        AiChatCommand command = new AiChatCommand(request.message().trim(), request.context(), boundedHistory(request),
                requestId, compactContext(context));
        if (route.ragAllowed()) {
            RagChatResult result = rag.chat(command);
            return response(result.status(), result.answer(), result.sources(), result.grounding(), requestId);
        }
        AiChatResult result = provider.chat(command);
        return response(result.status(), result.answer(), List.of(), new AiGrounding("NOT_ENABLED", false), requestId);
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

    private List<com.ieltsaitutor.ai.dto.ChatHistoryItem> boundedHistory(com.ieltsaitutor.ai.dto.AiChatRequest request) {
        List<com.ieltsaitutor.ai.dto.ChatHistoryItem> history = request.history();
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
