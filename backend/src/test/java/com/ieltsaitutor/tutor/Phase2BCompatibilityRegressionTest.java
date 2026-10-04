package com.ieltsaitutor.tutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.rag.chat.RagChatService;
import com.ieltsaitutor.tutor.context.TutorContextService;
import com.ieltsaitutor.tutor.context.TutorLearningContext;
import com.ieltsaitutor.tutor.intent.DefaultTutorIntentRouter;
import com.ieltsaitutor.tutor.memory.AiConversation;
import com.ieltsaitutor.tutor.memory.ConversationService;
import com.ieltsaitutor.tutor.memory.ConversationStatus;
import com.ieltsaitutor.tutor.tool.DeterministicTutorTools;

class Phase2BCompatibilityRegressionTest {
    @Test
    void normalizedChatResponsePreservesAdditiveConversationReference() {
        UUID user = UUID.randomUUID();
        AiConversation conversation = new AiConversation(UUID.randomUUID(), user, "general", null, null, null,
                "hello", ConversationStatus.ACTIVE, Instant.now(), Instant.now());
        ConversationService conversations = mock(ConversationService.class);
        when(conversations.create(any(), any(), any(), any(), any(), any())).thenReturn(conversation);
        AiProvider provider = mock(AiProvider.class);
        when(provider.chat(any())).thenReturn(AiChatResult.answered("Hello"));
        TutorContextService contexts = mock(TutorContextService.class);
        when(contexts.resolve(any(), any())).thenReturn(TutorLearningContext.absent("general"));
        var orchestrator = new TutorOrchestrator(provider, mock(RagChatService.class), contexts,
                new DefaultTutorIntentRouter(), mock(DeterministicTutorTools.class), conversations);

        var response = orchestrator.handle(new AuthPrincipal(user, "user@test", "User", UserRole.CUSTOMER),
                new AiChatRequest("hello", new AiChatContext("GENERAL", null, null, null, null, null, null), List.of()));

        assertThat(response.status()).isEqualTo("ANSWERED");
        assertThat(response.answer()).isEqualTo("Hello");
        assertThat(response.meta().conversationId()).isEqualTo(conversation.id());
        assertThat(response.sources()).isEmpty();
        assertThat(response.grounding().status()).isEqualTo("NOT_ENABLED");
    }
}
