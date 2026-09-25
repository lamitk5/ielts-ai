package com.ieltsaitutor.tutor;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.rag.chat.RagChatResult;
import com.ieltsaitutor.rag.chat.RagChatService;
import com.ieltsaitutor.ai.dto.AiGrounding;
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

class TutorOrchestratorTest {
    private final AiProvider provider = mock(AiProvider.class);
    private final RagChatService rag = mock(RagChatService.class);
    private final TutorContextService contexts = mock(TutorContextService.class);
    private final TutorIntentRouter intents = mock(TutorIntentRouter.class);
    private final DeterministicTutorTools tools = mock(DeterministicTutorTools.class);
    private final TutorOrchestrator orchestrator = new TutorOrchestrator(provider, rag, contexts, intents, tools);
    private final AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "user@test", "User", UserRole.CUSTOMER);

    @Test
    void deterministicApplicationDataDoesNotCallProviderOrRag() {
        TutorLearningContext context = TutorLearningContext.absent("reading");
        when(contexts.resolve(any(), any())).thenReturn(context);
        when(intents.route(any(), any())).thenReturn(new TutorIntentRoute(TutorIntent.APP_DATA, false, false, "data"));
        when(tools.execute(any(), any())).thenReturn(new TutorToolResult("APP_DATA", "Bạn đang làm câu q1."));

        assertThat(orchestrator.handle(principal, request("Tôi đang làm câu nào?")).status()).isEqualTo("APP_DATA");
        verifyNoInteractions(provider, rag);
    }

    @Test
    void genericChatUsesProviderWithoutRag() {
        when(contexts.resolve(any(), any())).thenReturn(TutorLearningContext.absent("general"));
        when(intents.route(any(), any())).thenReturn(new TutorIntentRoute(TutorIntent.GENERIC_CHAT, true, false, "generic"));
        when(provider.chat(any())).thenReturn(AiChatResult.answered("Hello"));

        assertThat(orchestrator.handle(null, request("hello")).answer()).isEqualTo("Hello");
        verify(provider).chat(any());
        verifyNoInteractions(rag);
    }

    @Test
    void ragResultPreservesGroundingAndSources() {
        when(contexts.resolve(any(), any())).thenReturn(TutorLearningContext.absent("general"));
        when(intents.route(any(), any())).thenReturn(new TutorIntentRoute(TutorIntent.RAG_EXPLANATION, true, true, "source"));
        when(rag.chat(any())).thenReturn(new RagChatResult("ANSWERED", "Grounded", List.of(), new AiGrounding("GROUNDED", true)));

        var result = orchestrator.handle(null, request("Explain the rubric with sources"));
        assertThat(result.grounding().status()).isEqualTo("GROUNDED");
        verify(rag).chat(any());
        verifyNoInteractions(provider);
    }

    private AiChatRequest request(String message) {
        return new AiChatRequest(message, new AiChatContext("GENERAL", null, null, null, null, null, null), List.of());
    }
}
