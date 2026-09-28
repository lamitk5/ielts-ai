package com.ieltsaitutor.tutor.memory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.attachment.AttachmentKind;
import com.ieltsaitutor.ai.attachment.TutorAttachment;
import com.ieltsaitutor.ai.attachment.TutorAttachmentCleanupService;
import com.ieltsaitutor.ai.attachment.TutorAttachmentRepository;
import com.ieltsaitutor.ai.attachment.TutorAttachmentHistoryView;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;

class ConversationAttachmentHistoryTest {
    private final ConversationRepository repository = mock(ConversationRepository.class);
    private final TutorAttachmentRepository attachments = mock(TutorAttachmentRepository.class);
    private final TutorAttachmentCleanupService cleanup = mock(TutorAttachmentCleanupService.class);
    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final ConversationService service = new ConversationService(repository, attachments, cleanup);

    @Test
    void linksReadyAttachmentsAfterSuccessfulSend() {
        AiMessage message = message();
        UUID attachmentId = UUID.randomUUID();
        when(repository.findConversation(userId, conversationId)).thenReturn(Optional.of(conversation()));
        when(attachments.findOwnedByIds(userId, conversationId, List.of(attachmentId)))
                .thenReturn(List.of(attachment(attachmentId, TutorAttachment.AttachmentStatus.READY)));

        service.appendMessageWithAttachments(userId, conversationId, message, List.of(attachmentId));

        verify(repository).saveMessageWithAttachments(userId, conversationId, message, List.of(attachmentId));
    }

    @Test
    void doesNotLinkFailedAttachments() {
        AiMessage message = message();
        UUID attachmentId = UUID.randomUUID();
        when(repository.findConversation(userId, conversationId)).thenReturn(Optional.of(conversation()));
        when(attachments.findOwnedByIds(userId, conversationId, List.of(attachmentId)))
                .thenReturn(List.of(attachment(attachmentId, TutorAttachment.AttachmentStatus.FAILED)));

        service.appendMessageWithAttachments(userId, conversationId, message, List.of(attachmentId));

        verify(repository).saveMessage(message);
        verify(repository, never()).saveMessageWithAttachments(any(), any(), any(), any());
    }

    @Test
    void reloadReturnsSafeMetadata() {
        AiMessage message = new AiMessage(UUID.randomUUID(), conversationId, 2, AiMessageRole.USER,
                "describe", "USER_MESSAGE", null, List.of(), java.util.Map.of(), Instant.now(),
                List.of(new TutorAttachmentHistoryView(UUID.randomUUID(), "notes.pdf", AttachmentKind.DOCUMENT,
                        42, TutorAttachment.AttachmentStatus.READY, "notes.pdf")));
        when(repository.findConversation(userId, conversationId)).thenReturn(Optional.of(conversation()));
        when(repository.findMessages(userId, conversationId)).thenReturn(List.of(message));

        AiMessage loaded = service.messages(userId, conversationId).getFirst();

        org.assertj.core.api.Assertions.assertThat(loaded.attachments()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(loaded.attachments().getFirst().filename()).isEqualTo("notes.pdf");
    }

    @Test
    void doesNotLoadBinaryBodies() {
        AiMessage message = message();
        when(repository.findConversation(userId, conversationId)).thenReturn(Optional.of(conversation()));
        when(repository.findMessages(userId, conversationId)).thenReturn(List.of(message));

        service.messages(userId, conversationId);

        verify(attachments, never()).findOwned(any(), any(), any());
    }

    @Test
    void messageAttachmentOrdinalIsStable() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        AiMessage message = message();
        when(repository.findConversation(userId, conversationId)).thenReturn(Optional.of(conversation()));
        when(attachments.findOwnedByIds(userId, conversationId, List.of(first, second))).thenReturn(List.of(
                attachment(first, TutorAttachment.AttachmentStatus.READY), attachment(second, TutorAttachment.AttachmentStatus.READY)));

        service.appendMessageWithAttachments(userId, conversationId, message, List.of(first, second));

        verify(repository).saveMessageWithAttachments(userId, conversationId, message, List.of(first, second));
    }

    @Test
    void conversationDeletionSchedulesUnreferencedCleanup() {
        when(repository.delete(userId, conversationId)).thenReturn(true);
        when(cleanup.cleanupRemovedOrExpired()).thenReturn(1);

        org.assertj.core.api.Assertions.assertThat(service.delete(userId, conversationId)).isTrue();

        verify(cleanup).cleanupRemovedOrExpired();
    }

    private AiMessage message() {
        return new AiMessage(UUID.randomUUID(), conversationId, 1, AiMessageRole.USER, "hello",
                "USER_MESSAGE", null, List.of(), java.util.Map.of(), Instant.now());
    }

    private AiConversation conversation() {
        Instant now = Instant.now();
        return new AiConversation(conversationId, userId, "general", null, null, null, "Tutor",
                ConversationStatus.ACTIVE, now, now);
    }

    private TutorAttachment attachment(UUID id, TutorAttachment.AttachmentStatus status) {
        Instant now = Instant.now();
        return new TutorAttachment(id, userId, conversationId, "notes.txt", "notes.txt", "text/plain",
                AttachmentKind.DOCUMENT, 8, "sha", "storage/" + id, status, null, null, 0, now, now, null);
    }
}
