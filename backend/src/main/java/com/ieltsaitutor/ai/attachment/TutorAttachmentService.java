package com.ieltsaitutor.ai.attachment;

import java.time.Instant;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ieltsaitutor.ai.attachment.TutorAttachment.AttachmentStatus;
import com.ieltsaitutor.auth.AuthException;

@Service
public class TutorAttachmentService {
    public static final long MAX_FILE_SIZE_BYTES = TutorAttachmentContract.MAX_FILE_SIZE_BYTES;

    private final Map<UUID, Map<UUID, TutorAttachment>> store = new ConcurrentHashMap<>();
    private final TutorAttachmentRepository repository;
    private final TutorAttachmentStorage storage;
    private final TutorAttachmentValidator validator;

    public TutorAttachmentService() {
        this.repository = null;
        this.storage = null;
        this.validator = null;
    }

    @Autowired
    public TutorAttachmentService(TutorAttachmentRepository repository, TutorAttachmentStorage storage,
            TutorAttachmentValidator validator) {
        this.repository = repository;
        this.storage = storage;
        this.validator = validator;
    }

    public List<TutorAttachment> uploadBatch(UUID userId, UUID conversationId, List<MultipartFile> files) {
        if (conversationId == null || files == null || files.isEmpty() || files.size() > 5) {
            throw new AuthException("ATTACHMENT_BATCH_INVALID", HttpStatus.BAD_REQUEST, "Chỉ có thể đính kèm từ 1 đến 5 tệp.");
        }
        return files.stream().map(file -> upload(userId, conversationId, file)).toList();
    }

    public TutorAttachment upload(UUID userId, UUID conversationId, MultipartFile file) {
        if (repository == null || storage == null || validator == null) {
            return upload(userId, file);
        }
        TutorAttachmentValidationResult result = validator.validate(file);
        if (!result.valid()) {
            throw validationError(result);
        }
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String storageKey = userId + "/" + conversationId + "/" + id + ".bin";
        try {
            storage.store(new ByteArrayInputStream(result.content()), storageKey, result.sizeBytes());
            TutorAttachment attachment = new TutorAttachment(id, userId, conversationId, result.canonicalFilename(),
                    result.canonicalFilename(), result.contentType(), result.kind(), result.sizeBytes(), sha256(result.content()),
                    storageKey, AttachmentStatus.STORED, null, null, 0, now, now, null);
            repository.save(attachment);
            return attachment;
        } catch (Exception exception) {
            try { storage.delete(storageKey); } catch (Exception ignored) { }
            throw new AuthException("ATTACHMENT_STORAGE_FAILED", HttpStatus.BAD_REQUEST, "Không thể lưu tệp đính kèm.");
        }
    }

    public TutorAttachment get(UUID userId, UUID conversationId, UUID attachmentId) {
        if (repository == null) return get(userId, attachmentId);
        return repository.findOwned(userId, conversationId, attachmentId)
                .orElseThrow(() -> new AuthException("ATTACHMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy tệp đính kèm."));
    }

    public void delete(UUID userId, UUID conversationId, UUID attachmentId) {
        if (repository == null) { delete(userId, attachmentId); return; }
        if (repository.findOwned(userId, conversationId, attachmentId).isEmpty()) {
            throw new AuthException("ATTACHMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy tệp đính kèm.");
        }
        repository.updateStatus(attachmentId, AttachmentStatus.REMOVED, null);
    }

    public TutorAttachment upload(UUID userId, MultipartFile file) {
        return upload(userId, file, null);
    }

    public TutorAttachment upload(UUID userId, MultipartFile file, String requestId) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new AuthException("ATTACHMENT_EMPTY", HttpStatus.BAD_REQUEST, "Tệp đính kèm không có nội dung.");
        }

        if (file.getSize() > TutorAttachmentContract.MAX_FILE_SIZE_BYTES) {
            throw new AuthException("ATTACHMENT_SIZE_EXCEEDED", HttpStatus.BAD_REQUEST, "Dung lượng tệp vượt quá 10MB.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = validateFilename(originalFilename);
        String contentType = file.getContentType();
        validateContentType(extension, contentType);
        String normalizedRequestId = normalizeRequestId(requestId);

        Map<UUID, TutorAttachment> userMap = store.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
        boolean hasActive = userMap.values().stream().anyMatch(existing ->
                isActive(existing) && (normalizedRequestId == null || normalizedRequestId.equals(existing.requestId())));
        if (hasActive) {
            throw new AuthException("ATTACHMENT_LIMIT_EXCEEDED", HttpStatus.CONFLICT,
                    "Mỗi yêu cầu chỉ hỗ trợ tối đa 1 tệp đính kèm đang hoạt động.");
        }

        UUID attachmentId = UUID.randomUUID();
        Instant now = Instant.now();
        TutorAttachment attachment = new TutorAttachment(
                attachmentId,
                userId,
                originalFilename,
                contentType != null ? contentType : "application/octet-stream",
                file.getSize(),
                TutorAttachmentContract.isImage(extension) ? AttachmentStatus.IMAGE_READY : AttachmentStatus.READY,
                null,
                now,
                now,
                normalizedRequestId,
                TutorAttachmentContract.isImage(extension) ? TutorAttachmentContract.VISION_NOT_ENABLED : null
        );

        userMap.put(attachmentId, attachment);
        return attachment;
    }

    public TutorAttachment get(UUID userId, UUID attachmentId) {
        Map<UUID, TutorAttachment> userMap = store.get(userId);
        if (userMap == null || !userMap.containsKey(attachmentId)) {
            throw new AuthException("ATTACHMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy tệp đính kèm.");
        }
        return userMap.get(attachmentId);
    }

    public void delete(UUID userId, UUID attachmentId) {
        Map<UUID, TutorAttachment> userMap = store.get(userId);
        if (userMap == null || !userMap.containsKey(attachmentId)) {
            throw new AuthException("ATTACHMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy tệp đính kèm.");
        }
        TutorAttachment existing = userMap.get(attachmentId);
        TutorAttachment updated = new TutorAttachment(
                existing.id(),
                existing.userId(),
                existing.filename(),
                existing.contentType(),
                existing.sizeBytes(),
                AttachmentStatus.REMOVED,
                null,
                existing.createdAt(),
                Instant.now()
        );
        userMap.put(attachmentId, updated);
        // Also remove from active store so subsequent uploads succeed
        userMap.remove(attachmentId);
    }

    private boolean isActive(TutorAttachment attachment) {
        return attachment.status() == AttachmentStatus.SELECTED
                || attachment.status() == AttachmentStatus.UPLOADING
                || attachment.status() == AttachmentStatus.UPLOADED
                || attachment.status() == AttachmentStatus.PROCESSING
                || attachment.status() == AttachmentStatus.READY
                || attachment.status() == AttachmentStatus.IMAGE_READY;
    }

    private String validateFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()
                || originalFilename.contains("/") || originalFilename.contains("\\")
                || originalFilename.indexOf('\0') >= 0) {
            throw unsupportedType();
        }
        String lowerFilename = originalFilename.toLowerCase(java.util.Locale.ROOT);
        if (TutorAttachmentContract.DISALLOWED_EXTENSIONS.stream().anyMatch(lowerFilename::endsWith)) {
            throw unsupportedType();
        }
        String extension = TutorAttachmentContract.extensionOf(originalFilename);
        if (extension.isBlank()) {
            throw unsupportedType();
        }
        return extension;
    }

    private void validateContentType(String extension, String contentType) {
        if (contentType == null || contentType.isBlank()) return;
        String normalized = contentType.toLowerCase(java.util.Locale.ROOT).trim();
        if (!TutorAttachmentContract.EXTENSION_MIME_TYPES.get(extension).contains(normalized)) {
            throw unsupportedType();
        }
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) return null;
        String normalized = requestId.trim();
        if (normalized.length() > 96 || !normalized.matches("[A-Za-z0-9_-]+")) {
            throw new AuthException("ATTACHMENT_REQUEST_INVALID", HttpStatus.BAD_REQUEST, "Yêu cầu đính kèm không hợp lệ.");
        }
        return normalized;
    }

    private AuthException unsupportedType() {
        return new AuthException("ATTACHMENT_TYPE_NOT_SUPPORTED", HttpStatus.BAD_REQUEST,
                "Định dạng tệp không được hỗ trợ. Chỉ chấp nhận PDF, DOCX, TXT hoặc PNG/JPG/WEBP.");
    }

    private AuthException validationError(TutorAttachmentValidationResult result) {
        HttpStatus status = result.errorCode().equals("ATTACHMENT_SIZE_EXCEEDED") ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_REQUEST;
        return new AuthException(result.errorCode(), status, result.errorMessage());
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
