package com.ieltsaitutor.tutor.memory;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ieltsaitutor.ai.attachment.TutorAttachment;
import com.ieltsaitutor.ai.attachment.TutorAttachmentCleanupService;
import com.ieltsaitutor.ai.attachment.TutorAttachmentRepository;

@Service
public class ConversationService {
    private final ConversationRepository repository;
    private final TutorAttachmentRepository attachments;
    private final TutorAttachmentCleanupService cleanup;

    public ConversationService(ConversationRepository repository) { this(repository, null, null); }

    @org.springframework.beans.factory.annotation.Autowired
    public ConversationService(ConversationRepository repository, TutorAttachmentRepository attachments,
            TutorAttachmentCleanupService cleanup) {
        this.repository = repository;
        this.attachments = attachments;
        this.cleanup = cleanup;
    }

    public AiConversation create(UUID userId, String skill, String practiceSetId, UUID attemptId, String questionId, String title) {
        Instant now = Instant.now();
        AiConversation result = new AiConversation(UUID.randomUUID(), userId, skill, practiceSetId, attemptId, questionId,
                title == null || title.isBlank() ? "Tutor conversation" : title.trim(), ConversationStatus.ACTIVE, now, now);
        if (userId != null) repository.saveConversation(result);
        return result;
    }

    public Optional<AiConversation> findOwned(UUID userId, UUID id) {
        return userId == null || id == null ? Optional.empty() : repository.findConversation(userId, id);
    }

    public void appendMessage(UUID userId, UUID conversationId, AiMessage message) {
        if (userId != null && findOwned(userId, conversationId).isPresent()) repository.saveMessage(message);
    }

    public void appendMessageWithAttachments(UUID userId, UUID conversationId, AiMessage message, java.util.List<UUID> attachmentIds) {
        if (userId == null || findOwned(userId, conversationId).isEmpty()) return;
        java.util.List<UUID> requestedIds = attachmentIds == null ? java.util.List.of() : java.util.List.copyOf(attachmentIds);
        java.util.Set<UUID> readyIds = attachments == null ? new java.util.LinkedHashSet<>(requestedIds)
                : attachments.findOwnedByIds(userId, conversationId, requestedIds).stream()
                        .filter(attachment -> attachment.status() == TutorAttachment.AttachmentStatus.READY)
                        .map(TutorAttachment::id)
                        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        java.util.List<UUID> orderedReadyIds = requestedIds.stream().filter(readyIds::contains).toList();
        if (orderedReadyIds.isEmpty()) repository.saveMessage(message);
        else repository.saveMessageWithAttachments(userId, conversationId, message, orderedReadyIds);
    }

    public java.util.List<AiConversation> listOwned(UUID userId) { return userId == null ? java.util.List.of() : repository.findConversations(userId); }
    public java.util.List<AiMessage> messages(UUID userId, UUID conversationId) { return findOwned(userId, conversationId).isPresent() ? repository.findMessages(userId, conversationId) : java.util.List.of(); }
    public boolean archive(UUID userId, UUID conversationId) { return userId != null && repository.archive(userId, conversationId); }
    public boolean delete(UUID userId, UUID conversationId) {
        boolean deleted = userId != null && repository.delete(userId, conversationId);
        if (deleted && cleanup != null) cleanup.cleanupRemovedOrExpired();
        return deleted;
    }
}
