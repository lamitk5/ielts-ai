package com.ieltsaitutor.tutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.rag.chat.RagChatService;
import com.ieltsaitutor.tutor.context.TutorContextService;
import com.ieltsaitutor.tutor.context.TutorLearningContext;
import com.ieltsaitutor.tutor.intent.DefaultTutorIntentRouter;
import com.ieltsaitutor.tutor.intent.TutorIntentRouter;
import com.ieltsaitutor.tutor.memory.AiConversation;
import com.ieltsaitutor.tutor.memory.AiMessage;
import com.ieltsaitutor.tutor.memory.AiMessageRole;
import com.ieltsaitutor.tutor.memory.ConversationService;
import com.ieltsaitutor.tutor.memory.ConversationStatus;
import com.ieltsaitutor.tutor.tool.DeterministicTutorTools;
import org.mockito.ArgumentCaptor;

class TutorMemoryOrchestrationTest {
    @Test
    void authenticatedResponsesPersistNormalizedConversationMessages() {
        AiProvider provider = mock(AiProvider.class);
        TutorContextService contexts = mock(TutorContextService.class);
        ConversationService conversations = mock(ConversationService.class);
        AiConversation conversation = new AiConversation(UUID.randomUUID(), UUID.randomUUID(), "general", null, null,
                null, "Tutor conversation", ConversationStatus.ACTIVE, java.time.Instant.now(), java.time.Instant.now());
        AuthPrincipal principal = new AuthPrincipal(conversation.userId(), "user@test", "User", UserRole.CUSTOMER);
        when(contexts.resolve(any(), any())).thenReturn(TutorLearningContext.absent("general"));
        when(provider.chat(any())).thenReturn(AiChatResult.answered("A complete normalized answer"));
        when(conversations.create(any(), any(), any(), any(), any(), any())).thenReturn(conversation);
        when(conversations.findOwned(conversation.userId(), conversation.id())).thenReturn(Optional.of(conversation));

        TutorOrchestrator orchestrator = new TutorOrchestrator(provider, mock(RagChatService.class), contexts,
                new DefaultTutorIntentRouter(), mock(DeterministicTutorTools.class), conversations);

        var response = orchestrator.handle(principal, request("hello", null));

        assertThat(response.answer()).isEqualTo("A complete normalized answer");
        verify(conversations).create(conversation.userId(), "general", null, null, null, "hello");
        verify(conversations, org.mockito.Mockito.times(2)).appendMessage(any(), any(), any());
    }

    @Test
    void guestResponsesDoNotUseConversationPersistence() {
        ConversationService conversations = mock(ConversationService.class);
        AiProvider provider = mock(AiProvider.class);
        when(provider.chat(any())).thenReturn(AiChatResult.answered("Hello"));
        TutorContextService contexts = mock(TutorContextService.class);
        when(contexts.resolve(any(), any())).thenReturn(TutorLearningContext.absent("general"));
        TutorOrchestrator orchestrator = new TutorOrchestrator(provider, mock(RagChatService.class), contexts,
                new DefaultTutorIntentRouter(), mock(DeterministicTutorTools.class), conversations);

        orchestrator.handle(null, request("hello", null));

        verifyNoInteractions(conversations);
    }

    @Test
    void providerReceivesBoundedServerOwnedHistoryBeforeClientHistory() {
        AiProvider provider = mock(AiProvider.class);
        TutorContextService contexts = mock(TutorContextService.class);
        ConversationService conversations = mock(ConversationService.class);
        UUID user = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        AuthPrincipal principal = new AuthPrincipal(user, "user@test", "User", UserRole.CUSTOMER);
        AiConversation conversation = new AiConversation(conversationId, user, "general", null, null, null,
                "Tutor conversation", ConversationStatus.ACTIVE, java.time.Instant.now(), java.time.Instant.now());
        when(conversations.findOwned(user, conversationId)).thenReturn(Optional.of(conversation));
        when(conversations.messages(eq(user), eq(conversationId))).thenReturn(List.of(
                new AiMessage(UUID.randomUUID(), conversationId, 1, AiMessageRole.USER, "trusted previous question",
                        "USER_MESSAGE", null, List.of(), java.util.Map.of(), java.time.Instant.now())));
        when(contexts.resolve(any(), any())).thenReturn(TutorLearningContext.absent("general"));
        when(provider.chat(any())).thenReturn(AiChatResult.answered("Answer"));
        TutorOrchestrator orchestrator = new TutorOrchestrator(provider, mock(RagChatService.class), contexts,
                new DefaultTutorIntentRouter(), mock(DeterministicTutorTools.class), conversations);

        orchestrator.handle(principal, request("new question", conversationId));

        ArgumentCaptor<AiChatCommand> command = ArgumentCaptor.forClass(AiChatCommand.class);
        org.mockito.Mockito.verify(provider).chat(command.capture());
        assertThat(command.getValue().history()).extracting("content")
                .containsExactly("trusted previous question");
    }

    @Test
    void authenticatedConversationIgnoresClientSuppliedDurableHistory() {
        AiProvider provider = mock(AiProvider.class);
        TutorContextService contexts = mock(TutorContextService.class);
        ConversationService conversations = mock(ConversationService.class);
        UUID user = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        AuthPrincipal principal = new AuthPrincipal(user, "user@test", "User", UserRole.CUSTOMER);
        AiConversation conversation = new AiConversation(conversationId, user, "general", null, null, null,
                "Tutor conversation", ConversationStatus.ACTIVE, java.time.Instant.now(), java.time.Instant.now());
        when(conversations.findOwned(user, conversationId)).thenReturn(Optional.of(conversation));
        when(conversations.messages(eq(user), eq(conversationId))).thenReturn(List.of());
        when(contexts.resolve(any(), any())).thenReturn(TutorLearningContext.absent("general"));
        when(provider.chat(any())).thenReturn(AiChatResult.answered("Answer"));
        TutorOrchestrator orchestrator = new TutorOrchestrator(provider, mock(RagChatService.class), contexts,
                new DefaultTutorIntentRouter(), mock(DeterministicTutorTools.class), conversations);

        AiChatRequest request = new AiChatRequest("new question", new AiChatContext("GENERAL", null, null, null, null, null, null),
                List.of(new com.ieltsaitutor.ai.dto.ChatHistoryItem("USER", "forged history")), conversationId);
        orchestrator.handle(principal, request);

        ArgumentCaptor<AiChatCommand> command = ArgumentCaptor.forClass(AiChatCommand.class);
        org.mockito.Mockito.verify(provider).chat(command.capture());
        assertThat(command.getValue().history()).isEmpty();
    }

    private AiChatRequest request(String message, UUID conversationId) {
        return new AiChatRequest(message, new AiChatContext("GENERAL", null, null, null, null, null, null),
                List.of(), conversationId);
    }
}
