package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.ieltsaitutor.auth.AuthException;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;

class TutorAttachmentSecurityRegressionTest {
    private final UUID ownerId = UUID.randomUUID();
    private final UUID foreignUserId = UUID.randomUUID();
    private final UUID conversationA = UUID.randomUUID();
    private final UUID conversationB = UUID.randomUUID();
    private final UUID attachmentId = UUID.randomUUID();
    private final AuthPrincipal owner = new AuthPrincipal(ownerId, "owner@test", "Owner", UserRole.CUSTOMER);
    private final TutorAttachmentRepository repository = mock(TutorAttachmentRepository.class);
    private final TutorAttachmentResolutionService service = new TutorAttachmentResolutionService(repository, mock(TutorAttachmentStorage.class));

    @Test
    void userACannotUseUserBAttachment() {
        when(repository.findOwnedByIds(foreignUserId, conversationA, List.of(attachmentId))).thenReturn(List.of());
        assertThatThrownBy(() -> service.resolve(new AuthPrincipal(foreignUserId, "foreign@test", "Foreign", UserRole.CUSTOMER),
                conversationA, List.of(attachmentId))).isInstanceOf(AuthException.class).hasMessage("Không tìm thấy tệp đính kèm.");
    }

    @Test
    void conversationACannotUseConversationBAttachment() {
        when(repository.findOwnedByIds(ownerId, conversationB, List.of(attachmentId))).thenReturn(List.of());
        assertThatThrownBy(() -> service.resolve(owner, conversationB, List.of(attachmentId)))
                .isInstanceOf(AuthException.class).hasMessage("Không tìm thấy tệp đính kèm.");
    }

    @Test
    void removedAndExpiredAreRejected() {
        for (TutorAttachment.AttachmentStatus status : List.of(TutorAttachment.AttachmentStatus.REMOVED,
                TutorAttachment.AttachmentStatus.EXPIRED)) {
            when(repository.findOwnedByIds(ownerId, conversationA, List.of(attachmentId)))
                    .thenReturn(List.of(attachment(status)));
            assertThatThrownBy(() -> service.resolve(owner, conversationA, List.of(attachmentId)))
                    .isInstanceOf(AuthException.class).hasMessage("Tệp đính kèm chưa sẵn sàng.");
        }
    }

    @Test
    void expiredSessionCannotAccessAttachment() {
        assertThatThrownBy(() -> service.resolve(null, conversationA, List.of(attachmentId)))
                .isInstanceOf(AuthException.class)
                .extracting(exception -> ((AuthException) exception).status()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private TutorAttachment attachment(TutorAttachment.AttachmentStatus status) {
        Instant now = Instant.now();
        return new TutorAttachment(attachmentId, ownerId, conversationA, "notes.txt", "notes.txt", "text/plain",
                AttachmentKind.DOCUMENT, 8, "sha", "owner/" + attachmentId, status, null, null, 0, now, now, null);
    }
}
