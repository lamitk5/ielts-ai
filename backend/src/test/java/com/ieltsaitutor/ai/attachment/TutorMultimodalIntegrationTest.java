package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.ai.provider.ProviderCapability;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.rag.chat.RagChatService;
import com.ieltsaitutor.tutor.TutorOrchestrator;
import com.ieltsaitutor.tutor.context.TutorContextService;
import com.ieltsaitutor.tutor.intent.TutorIntent;
import com.ieltsaitutor.tutor.intent.TutorIntentRoute;
import com.ieltsaitutor.tutor.intent.TutorIntentRouter;
import com.ieltsaitutor.tutor.security.TutorRateLimiter;
import com.ieltsaitutor.tutor.tool.DeterministicTutorTools;

class TutorMultimodalIntegrationTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final AuthPrincipal principal = new AuthPrincipal(userId, "learner@test", "Learner", UserRole.CUSTOMER);
    private final AiProvider provider = mock(AiProvider.class);
    private final TutorAttachmentRepository repository = mock(TutorAttachmentRepository.class);
    private final TutorAttachmentStorage storage = mock(TutorAttachmentStorage.class);
    private final TutorAttachmentResolutionService resolution = new TutorAttachmentResolutionService(repository, storage);
    private final TutorAttachmentContextBuilder contexts = mock(TutorAttachmentContextBuilder.class);
    private final TutorContextService tutorContext = mock(TutorContextService.class);
    private final TutorIntentRouter intents = mock(TutorIntentRouter.class);
    private final DeterministicTutorTools tools = mock(DeterministicTutorTools.class);

    @Test
    void imageUsesActualPixels() throws Exception {
        UUID imageId = UUID.randomUUID();
        byte[] pixels = new byte[] { 1, 2, 3, 4 };
        when(repository.findOwnedByIds(userId, conversationId, List.of(imageId))).thenReturn(List.of(attachment(imageId, AttachmentKind.IMAGE, "photo.png")));
        when(storage.open("storage-" + imageId)).thenReturn(new ByteArrayInputStream(pixels));
        when(contexts.build(any(), any())).thenReturn(new AttachmentContext(TutorAttachmentQuestionMode.FOCUSED, "", List.of(), List.of(), 0));
        givenGenericRoute();
        AtomicReference<AiChatCommand> captured = new AtomicReference<>();
        when(provider.chat(any())).thenAnswer(invocation -> { captured.set(invocation.getArgument(0)); return AiChatResult.answered("seen"); });

        newOrchestrator().handle(principal, request("Ảnh này có gì?", List.of(imageId)));

        assertThat(captured.get().requiredCapabilities()).contains(ProviderCapability.VISION_IMAGE);
        try (var stream = captured.get().attachments().getFirst().openStream().get()) {
            assertThat(stream.readAllBytes()).containsExactly(pixels);
        }
    }

    @Test
    void textOnlyProviderCannotReceiveVisionTurn() {
        when(provider.chat(any())).thenReturn(AiChatResult.answered("hello"));
        givenGenericRoute();

        newOrchestrator().handle(principal, new AiChatRequest("hello", new AiChatContext("GENERAL", null, null, null, null, null, null), List.of()));

        org.mockito.ArgumentCaptor<AiChatCommand> captor = org.mockito.ArgumentCaptor.forClass(AiChatCommand.class);
        verify(provider).chat(captor.capture());
        assertThat(captor.getValue().attachments()).isEmpty();
        assertThat(captor.getValue().requiredCapabilities()).containsExactly(ProviderCapability.CHAT);
    }

    @Test
    void mixedImagePdfDocxPreservesSources() throws IOException {
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(repository.findOwnedByIds(userId, conversationId, ids)).thenReturn(List.of(
                attachment(ids.get(0), AttachmentKind.IMAGE, "photo.png"),
                attachment(ids.get(1), AttachmentKind.DOCUMENT, "essay.pdf"),
                attachment(ids.get(2), AttachmentKind.DOCUMENT, "notes.docx")));
        when(storage.open(any())).thenReturn(new ByteArrayInputStream(new byte[] { 9 }));
        when(contexts.build(any(), any())).thenReturn(new AttachmentContext(TutorAttachmentQuestionMode.WHOLE_DOCUMENT,
                "document evidence", List.of(), List.of(), 4));
        givenGenericRoute();
        when(provider.chat(any())).thenReturn(AiChatResult.answered("mixed"));

        var result = newOrchestrator().handle(principal, request("Compare these", ids));

        assertThat(result.attachmentSources()).extracting(source -> source.filename())
                .containsExactly("photo.png", "essay.pdf", "notes.docx");
    }

    @Test
    void providerFailureKeepsAttachmentReady() throws Exception {
        UUID imageId = UUID.randomUUID();
        when(repository.findOwnedByIds(userId, conversationId, List.of(imageId))).thenReturn(List.of(attachment(imageId, AttachmentKind.IMAGE, "photo.png")));
        when(storage.open("storage-" + imageId)).thenReturn(new ByteArrayInputStream(new byte[] { 1 }));
        when(contexts.build(any(), any())).thenReturn(new AttachmentContext(TutorAttachmentQuestionMode.FOCUSED, "", List.of(), List.of(), 0));
        givenGenericRoute();
        when(provider.chat(any())).thenThrow(new com.ieltsaitutor.ai.exception.AiProviderException("AI_TEMPORARILY_UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE, "down"));

        assertThatThrownBy(() -> newOrchestrator().handle(principal, request("Describe", List.of(imageId))))
                .isInstanceOf(com.ieltsaitutor.ai.exception.AiProviderException.class);
        verify(repository, never()).updateStatus(any(), any(), any());
    }

    private TutorOrchestrator newOrchestrator() {
        return new TutorOrchestrator(provider, mock(RagChatService.class), tutorContext, intents, tools,
                new TutorRateLimiter(10, 10, java.time.Duration.ofMinutes(1)), null, resolution, contexts);
    }

    private void givenGenericRoute() {
        when(tutorContext.resolve(any(), any())).thenReturn(com.ieltsaitutor.tutor.context.TutorLearningContext.absent("general"));
        when(intents.route(any(), any())).thenReturn(new TutorIntentRoute(TutorIntent.GENERIC_CHAT, true, false, "generic"));
    }

    private AiChatRequest request(String message, List<UUID> ids) {
        return new AiChatRequest(message, new AiChatContext("GENERAL", null, null, null, null, null, null), List.of(), conversationId, ids);
    }

    private TutorAttachment attachment(UUID id, AttachmentKind kind, String filename) {
        Instant now = Instant.now();
        return new TutorAttachment(id, userId, conversationId, filename, filename, kind == AttachmentKind.IMAGE ? "image/png" : "application/pdf",
                kind, 4, "sha", "storage-" + id, TutorAttachment.AttachmentStatus.READY, null, null, 0, now, now, null);
    }
}
