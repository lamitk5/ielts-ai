package com.ieltsaitutor.tutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.attachment.AttachmentKind;
import com.ieltsaitutor.ai.attachment.AttachmentContext;
import com.ieltsaitutor.ai.attachment.AttachmentChatScope;
import com.ieltsaitutor.ai.attachment.TutorAttachment;
import com.ieltsaitutor.ai.attachment.TutorAttachmentChunkRepository;
import com.ieltsaitutor.ai.attachment.TutorAttachmentContextBuilder;
import com.ieltsaitutor.ai.attachment.TutorAttachmentChatContext;
import com.ieltsaitutor.ai.attachment.TutorAttachmentRepository;
import com.ieltsaitutor.ai.attachment.TutorAttachmentResolutionService;
import com.ieltsaitutor.ai.attachment.TutorAttachmentStorage;
import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.ai.exception.AiProviderException;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.rag.chat.RagChatService;
import com.ieltsaitutor.tutor.context.TutorContextService;
import com.ieltsaitutor.tutor.intent.TutorIntent;
import com.ieltsaitutor.tutor.intent.TutorIntentRoute;
import com.ieltsaitutor.tutor.intent.TutorIntentRouter;
import com.ieltsaitutor.tutor.security.TutorRateLimiter;
import com.ieltsaitutor.tutor.tool.DeterministicTutorTools;

class TutorAttachmentOrchestrationTest {
    private final AiProvider provider = mock(AiProvider.class);
    private final RagChatService rag = mock(RagChatService.class);
    private final TutorContextService contexts = mock(TutorContextService.class);
    private final TutorIntentRouter intents = mock(TutorIntentRouter.class);
    private final DeterministicTutorTools tools = mock(DeterministicTutorTools.class);
    private final TutorAttachmentResolutionService resolution = mock(TutorAttachmentResolutionService.class);
    private final TutorAttachmentContextBuilder contextBuilder = mock(TutorAttachmentContextBuilder.class);
    private final AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "learner@test", "Learner", UserRole.CUSTOMER);

    @Test
    void textOnlyRequestSkipsAttachmentResolution() {
        givenGenericRoute();
        when(provider.chat(any())).thenReturn(AiChatResult.answered("hello"));

        assertThat(orchestrator().handle(principal, request("hello", List.of())).answer()).isEqualTo("hello");
        verify(resolution, never()).resolve(any(), any(), anyList());
    }

    @Test
    void rejectsMoreThanFiveIds() {
        List<UUID> ids = java.util.stream.IntStream.range(0, 6).mapToObj(i -> UUID.randomUUID()).toList();
        TutorAttachmentRepository repository = mock(TutorAttachmentRepository.class);
        TutorAttachmentResolutionService service = new TutorAttachmentResolutionService(repository, mock(TutorAttachmentStorage.class));

        assertThatThrownBy(() -> service.resolve(principal, UUID.randomUUID(), ids))
                .isInstanceOf(com.ieltsaitutor.auth.AuthException.class)
                .hasMessage("Chỉ được đính kèm tối đa 5 tệp.");
    }

    @Test
    void rejectsDuplicateIds() {
        UUID id = UUID.randomUUID();
        TutorAttachmentResolutionService service = new TutorAttachmentResolutionService(mock(TutorAttachmentRepository.class),
                mock(TutorAttachmentStorage.class));

        assertThatThrownBy(() -> service.resolve(principal, UUID.randomUUID(), List.of(id, id)))
                .isInstanceOf(com.ieltsaitutor.auth.AuthException.class)
                .hasMessage("Danh sách tệp đính kèm bị trùng.");
    }

    @Test
    void rejectsNonReadyAttachment() {
        UUID id = UUID.randomUUID();
        TutorAttachmentRepository repository = mock(TutorAttachmentRepository.class);
        when(repository.findOwnedByIds(principal.userId(), UUID.randomUUID(), List.of(id))).thenReturn(List.of());
        TutorAttachmentResolutionService service = new TutorAttachmentResolutionService(repository, mock(TutorAttachmentStorage.class));
        UUID conversation = UUID.randomUUID();
        when(repository.findOwnedByIds(principal.userId(), conversation, List.of(id))).thenReturn(List.of(attachment(id,
                TutorAttachment.AttachmentStatus.PROCESSING, AttachmentKind.DOCUMENT)));

        assertThatThrownBy(() -> service.resolve(principal, conversation, List.of(id)))
                .isInstanceOf(com.ieltsaitutor.auth.AuthException.class)
                .hasMessage("Tệp đính kèm chưa sẵn sàng.");
    }

    @Test
    void resolvesOwnedConversationScopedAttachments() throws Exception {
        UUID id = UUID.randomUUID();
        UUID conversation = UUID.randomUUID();
        TutorAttachmentRepository repository = mock(TutorAttachmentRepository.class);
        TutorAttachmentStorage storage = mock(TutorAttachmentStorage.class);
        when(repository.findOwnedByIds(principal.userId(), conversation, List.of(id))).thenReturn(List.of(attachment(id,
                TutorAttachment.AttachmentStatus.READY, AttachmentKind.IMAGE)));
        when(storage.open("key-" + id)).thenReturn(new ByteArrayInputStream(new byte[] { 1 }));

        TutorAttachmentResolutionService service = new TutorAttachmentResolutionService(repository, storage);
        assertThat(service.resolve(principal, conversation, List.of(id)).sources()).hasSize(1);
    }

    @Test
    void returnsInsufficientEvidenceForWeakDocumentContext() {
        UUID id = UUID.randomUUID();
        UUID conversation = UUID.randomUUID();
        TutorAttachmentChatContext resolved = new TutorAttachmentChatContext(
                new AttachmentChatScope(principal.userId(), conversation, List.of(id),
                        com.ieltsaitutor.ai.attachment.TutorAttachmentQuestionMode.FOCUSED), List.of(), List.of());
        when(resolution.resolve(principal, conversation, List.of(id))).thenReturn(resolved);
        when(contextBuilder.build(any(), any())).thenReturn(new AttachmentContext(
                com.ieltsaitutor.ai.attachment.TutorAttachmentQuestionMode.FOCUSED, "", List.of(), List.of(), 0));
        givenGenericRoute();

        assertThat(orchestrator().handle(principal, request("What does this document say?", conversation, List.of(id))).status())
                .isEqualTo("INSUFFICIENT_CONTEXT");
        verify(provider, never()).chat(any());
    }

    @Test
    void keepsProviderFailureAttachmentReady() throws Exception {
        UUID id = UUID.randomUUID();
        UUID conversation = UUID.randomUUID();
        TutorAttachment attachment = attachment(id, TutorAttachment.AttachmentStatus.READY, AttachmentKind.IMAGE);
        TutorAttachmentRepository repository = mock(TutorAttachmentRepository.class);
        when(repository.findOwnedByIds(principal.userId(), conversation, List.of(id))).thenReturn(List.of(attachment));
        TutorAttachmentStorage storage = mock(TutorAttachmentStorage.class);
        when(storage.open(attachment.storageKey())).thenReturn(new ByteArrayInputStream(new byte[] { 1 }));
        TutorAttachmentResolutionService realResolution = new TutorAttachmentResolutionService(repository, storage);
        when(provider.chat(any())).thenThrow(new AiProviderException("AI_TEMPORARILY_UNAVAILABLE",
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "down"));
        givenGenericRoute();
        TutorAttachmentContextBuilder builder = mock(TutorAttachmentContextBuilder.class);
        when(builder.build(any(), any())).thenReturn(new AttachmentContext(
                com.ieltsaitutor.ai.attachment.TutorAttachmentQuestionMode.FOCUSED, "evidence", List.of(), List.of(), 1));

        assertThatThrownBy(() -> orchestrator(realResolution, builder).handle(principal,
                request("Describe", conversation, List.of(id)))).isInstanceOf(AiProviderException.class);
        verify(repository, never()).updateStatus(any(), any(), any());
    }

    private TutorOrchestrator orchestrator() { return orchestrator(resolution, contextBuilder); }

    private TutorOrchestrator orchestrator(TutorAttachmentResolutionService resolved, TutorAttachmentContextBuilder builder) {
        return new TutorOrchestrator(provider, rag, contexts, intents, tools,
                new TutorRateLimiter(10, 10, java.time.Duration.ofMinutes(1)), null, resolved, builder);
    }

    private void givenGenericRoute() {
        when(contexts.resolve(any(), any())).thenReturn(com.ieltsaitutor.tutor.context.TutorLearningContext.absent("general"));
        when(intents.route(any(), any())).thenReturn(new TutorIntentRoute(TutorIntent.GENERIC_CHAT, true, false, "generic"));
    }

    private AiChatRequest request(String message, List<UUID> ids) {
        return new AiChatRequest(message, new AiChatContext("GENERAL", null, null, null, null, null, null), List.of(), null, ids);
    }

    private AiChatRequest request(String message, UUID conversationId, List<UUID> ids) {
        return new AiChatRequest(message, new AiChatContext("GENERAL", null, null, null, null, null, null), List.of(), conversationId, ids);
    }

    private TutorAttachment attachment(UUID id, TutorAttachment.AttachmentStatus status, AttachmentKind kind) {
        Instant now = Instant.now();
        return new TutorAttachment(id, principal.userId(), UUID.randomUUID(), "file." + (kind == AttachmentKind.IMAGE ? "png" : "txt"),
                "file", kind == AttachmentKind.IMAGE ? "image/png" : "text/plain", kind, 1, "sha", "key-" + id, status,
                null, null, 0, now, now, null);
    }
}
