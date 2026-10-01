package com.ieltsaitutor.ai.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.ai.dto.AiAttachmentSource;
import com.ieltsaitutor.ai.model.AiAttachmentPart;
import com.ieltsaitutor.auth.AuthException;
import com.ieltsaitutor.auth.AuthPrincipal;

@Service
public class TutorAttachmentResolutionService {
    private final TutorAttachmentRepository repository;
    private final TutorAttachmentStorage storage;

    public TutorAttachmentResolutionService(TutorAttachmentRepository repository, TutorAttachmentStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    public TutorAttachmentChatContext resolve(AuthPrincipal principal, UUID conversationId, List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new AuthException("ATTACHMENT_IDS_REQUIRED", HttpStatus.BAD_REQUEST, "Cần chọn tệp đính kèm.");
        }
        if (principal == null) {
            throw new AuthException("ATTACHMENT_AUTH_REQUIRED", HttpStatus.UNAUTHORIZED, "Cần đăng nhập để dùng tệp đính kèm.");
        }
        if (conversationId == null) {
            throw new AuthException("ATTACHMENT_CONVERSATION_REQUIRED", HttpStatus.BAD_REQUEST,
                    "Tệp đính kèm cần thuộc một cuộc hội thoại.");
        }
        if (ids.size() > 5) {
            throw new AuthException("ATTACHMENT_LIMIT_EXCEEDED", HttpStatus.BAD_REQUEST, "Chỉ được đính kèm tối đa 5 tệp.");
        }
        if (Set.copyOf(ids).size() != ids.size()) {
            throw new AuthException("ATTACHMENT_IDS_DUPLICATED", HttpStatus.BAD_REQUEST, "Danh sách tệp đính kèm bị trùng.");
        }
        List<TutorAttachment> attachments = repository.findOwnedByIds(principal.userId(), conversationId, ids);
        if (attachments.size() != ids.size()) {
            throw new AuthException("ATTACHMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy tệp đính kèm.");
        }
        if (attachments.stream().anyMatch(attachment -> attachment.status() != TutorAttachment.AttachmentStatus.READY)) {
            throw new AuthException("ATTACHMENT_NOT_READY", HttpStatus.CONFLICT, "Tệp đính kèm chưa sẵn sàng.");
        }
        List<AiAttachmentPart> parts = attachments.stream().filter(attachment -> attachment.kind() == AttachmentKind.IMAGE)
                .map(this::imagePart).toList();
        List<AiAttachmentSource> sources = attachments.stream()
                .map(attachment -> new AiAttachmentSource(attachment.id(), attachment.filename(),
                        attachment.kind(), attachment.sizeBytes(), attachment.status())).toList();
        return new TutorAttachmentChatContext(new AttachmentChatScope(principal.userId(), conversationId, ids,
                TutorAttachmentQuestionMode.FOCUSED), parts, sources);
    }

    private AiAttachmentPart imagePart(TutorAttachment attachment) {
        Supplier<InputStream> opener = () -> {
            try { return storage.open(attachment.storageKey()); }
            catch (IOException exception) { throw new IllegalStateException("ATTACHMENT_READ_FAILED", exception); }
        };
        return new AiAttachmentPart(attachment.id(), attachment.filename(), attachment.contentType(), attachment.kind(), opener);
    }
}
