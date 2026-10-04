package com.ieltsaitutor.tutor.context;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.rag.chat.RagChatService;
import com.ieltsaitutor.tutor.TutorOrchestrator;
import com.ieltsaitutor.tutor.intent.TutorIntentRouter;
import com.ieltsaitutor.tutor.intent.TutorIntent;
import com.ieltsaitutor.tutor.intent.TutorIntentRoute;
import com.ieltsaitutor.tutor.tool.DeterministicTutorTools;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TutorContextTamperingTest {
    @Test
    void malformedPrivateAttemptReferenceIsRejectedInsteadOfDowngradedToGenericContext() {
        TutorContextService contexts = mock(TutorContextService.class);
        TutorIntentRouter intents = mock(TutorIntentRouter.class);
        AiProvider provider = mock(AiProvider.class);
        TutorOrchestrator orchestrator = new TutorOrchestrator(provider, mock(RagChatService.class),
                contexts, intents, mock(DeterministicTutorTools.class));
        when(contexts.resolve(any(), any())).thenReturn(TutorLearningContext.absent("speaking"));
        when(intents.route(any(), any())).thenReturn(new TutorIntentRoute(TutorIntent.GENERIC_CHAT, true, false, "generic"));
        when(provider.chat(any())).thenReturn(com.ieltsaitutor.ai.model.AiChatResult.answered("safe"));

        AiChatRequest request = new AiChatRequest("review my answer",
                new AiChatContext("SPEAKING", null, null, null, null, null, null, "not-a-uuid", "prompt-1"),
                List.of());

        assertThatThrownBy(() -> orchestrator.handle(
                new AuthPrincipal(UUID.randomUUID(), "user@example.test", "User", UserRole.CUSTOMER), request))
                .isInstanceOf(TutorContextException.class)
                .hasMessageContaining("Context Tutor");
    }
}
