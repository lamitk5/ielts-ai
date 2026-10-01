package com.ieltsaitutor.ai.attachment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TutorAttachmentRepository {
    void save(TutorAttachment attachment);
    Optional<TutorAttachment> findById(UUID attachmentId);
    Optional<TutorAttachment> findOwned(UUID userId, UUID conversationId, UUID attachmentId);
    List<TutorAttachment> findOwnedByIds(UUID userId, UUID conversationId, List<UUID> ids);
    int updateStatus(UUID attachmentId, TutorAttachment.AttachmentStatus status, String errorCode);
    List<TutorAttachment> findStaleProcessing(Instant cutoff);
    List<TutorAttachment> findRemovable();
}
