package com.ieltsaitutor.tutor;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.rag.chat.RagChatService;
import com.ieltsaitutor.tutor.context.TutorContextService;
import com.ieltsaitutor.tutor.context.TutorLearningContext;
import com.ieltsaitutor.tutor.intent.TutorIntent;
import com.ieltsaitutor.tutor.intent.TutorIntentRoute;
import com.ieltsaitutor.tutor.intent.TutorIntentRouter;
import com.ieltsaitutor.tutor.tool.DeterministicTutorTools;
import com.ieltsaitutor.tutor.tool.TutorToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TutorDeterministicIntentTest {
    @Test
    void currentQuestionAnswerAndScoreUseTrustedToolWithoutExternalAi() {
        AiProvider provider = mock(AiProvider.class);
        RagChatService rag = mock(RagChatService.class);
        TutorContextService contexts = mock(TutorContextService.class);
        TutorIntentRouter intents = mock(TutorIntentRouter.class);
        DeterministicTutorTools tools = mock(DeterministicTutorTools.class);
        TutorOrchestrator orchestrator = new TutorOrchestrator(provider, rag, contexts, intents, tools);
        when(contexts.resolve(any(), any())).thenReturn(new TutorLearningContext(true, "reading", "q", "q1", "prompt",
                "C", "B", "explanation", 1, 2, null, null, null, null, List.of(), null));
        when(intents.route(any(), any())).thenReturn(new TutorIntentRoute(TutorIntent.APP_DATA, false, false, "trusted"));
        when(tools.execute(any(), any())).thenReturn(new TutorToolResult("APP_DATA", "Bạn đã chọn C."));

        var result = orchestrator.handle(new AuthPrincipal(UUID.randomUUID(), "user@example.test", "User", UserRole.CUSTOMER),
                new AiChatRequest("đáp án tôi vừa chọn", new AiChatContext("READING", "set-1", "set-1", "q1", null, null, null), List.of()));

        assertThat(result.status()).isEqualTo("APP_DATA");
        verify(tools).execute(eq(TutorIntent.APP_DATA), any());
        verifyNoInteractions(provider, rag);
    }

    @Test
    void genericHelloUsesProviderAndSkipsRag() {
        AiProvider provider = mock(AiProvider.class);
        RagChatService rag = mock(RagChatService.class);
        TutorContextService contexts = mock(TutorContextService.class);
        TutorIntentRouter intents = mock(TutorIntentRouter.class);
        TutorOrchestrator orchestrator = new TutorOrchestrator(provider, rag, contexts, intents, mock(DeterministicTutorTools.class));
        when(contexts.resolve(any(), any())).thenReturn(TutorLearningContext.absent("general"));
        when(intents.route(any(), any())).thenReturn(new TutorIntentRoute(TutorIntent.GENERIC_CHAT, true, false, "generic"));
        when(provider.chat(any())).thenReturn(AiChatResult.answered("Hello"));

        assertThat(orchestrator.handle(null, new AiChatRequest("hello", new AiChatContext("GENERAL", null, null, null, null, null, null), List.of())).answer())
                .isEqualTo("Hello");
        verify(provider).chat(any());
        verifyNoInteractions(rag);
    }
}
