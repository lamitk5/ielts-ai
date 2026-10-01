package com.ieltsaitutor.tutor.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.rag.chat.RagChatService;
import com.ieltsaitutor.tutor.TutorOrchestrator;
import com.ieltsaitutor.tutor.context.TutorContextService;
import com.ieltsaitutor.tutor.context.TutorLearningContext;
import com.ieltsaitutor.tutor.intent.DefaultTutorIntentRouter;
import com.ieltsaitutor.tutor.tool.DeterministicTutorTools;

class Phase2BConversationSecurityRegressionTest {
    @Test
    void foreignConversationCannotReachProviderOrRag() {
        ConversationService conversations = mock(ConversationService.class);
        AiProvider provider = mock(AiProvider.class);
        RagChatService rag = mock(RagChatService.class);
        TutorContextService contexts = mock(TutorContextService.class);
        UUID user = UUID.randomUUID();
        when(conversations.findOwned(user, UUID.randomUUID())).thenReturn(Optional.empty());
        UUID foreignConversation = UUID.randomUUID();
        when(conversations.findOwned(user, foreignConversation)).thenReturn(Optional.empty());
        when(contexts.resolve(any(), any())).thenReturn(TutorLearningContext.absent("general"));
        var orchestrator = new TutorOrchestrator(provider, rag, contexts, new DefaultTutorIntentRouter(),
                mock(DeterministicTutorTools.class), conversations);

        var response = orchestrator.handle(new AuthPrincipal(user, "user@test", "User", UserRole.CUSTOMER),
                new AiChatRequest("hello", new AiChatContext("GENERAL", null, null, null, null, null, null), List.of(), foreignConversation));

        assertThat(response.status()).isEqualTo("INVALID_CONVERSATION");
        verifyNoInteractions(provider, rag, contexts);
    }

    @Test
    void guestRequestCannotPersistAsMemberConversation() {
        ConversationService conversations = mock(ConversationService.class);
        AiProvider provider = mock(AiProvider.class);
        when(provider.chat(any())).thenReturn(com.ieltsaitutor.ai.model.AiChatResult.answered("Hello"));
        TutorContextService contexts = mock(TutorContextService.class);
        when(contexts.resolve(any(), any())).thenReturn(TutorLearningContext.absent("general"));
        var orchestrator = new TutorOrchestrator(provider, mock(RagChatService.class), contexts,
                new DefaultTutorIntentRouter(), mock(DeterministicTutorTools.class), conversations);

        orchestrator.handle(null, new AiChatRequest("hello", new AiChatContext("GENERAL", null, null, null, null, null, null), List.of()));

        verifyNoInteractions(conversations);
    }
}
