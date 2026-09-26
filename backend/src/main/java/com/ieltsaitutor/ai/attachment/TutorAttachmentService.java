package com.ieltsaitutor.ai.attachment;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ieltsaitutor.ai.attachment.TutorAttachment.AttachmentStatus;
import com.ieltsaitutor.auth.AuthException;

@Service
public class TutorAttachmentService {
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L; // 10 MiB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx", ".txt");
    private static final Set<String> DISALLOWED_EXTENSIONS = Set.of(
            ".html", ".htm", ".exe", ".sh", ".bat", ".cmd", ".js", ".mjs", ".py", ".vbs", ".php"
    );
    private static final Set<String> DISALLOWED_MIME_PREFIXES = Set.of(
            "text/html", "application/x-msdownload", "application/x-sh", "application/javascript"
    );

    private final Map<UUID, Map<UUID, TutorAttachment>> store = new ConcurrentHashMap<>();

    public TutorAttachment upload(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new AuthException("ATTACHMENT_EMPTY", HttpStatus.BAD_REQUEST, "Tệp đính kèm không có nội dung.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AuthException("ATTACHMENT_SIZE_EXCEEDED", HttpStatus.BAD_REQUEST, "Dung lượng tệp vượt quá 10MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new AuthException("ATTACHMENT_TYPE_NOT_SUPPORTED", HttpStatus.BAD_REQUEST, "Tên tệp không hợp lệ.");
        }

        String lowerFilename = originalFilename.toLowerCase(Locale.ROOT);
        for (String disallowed : DISALLOWED_EXTENSIONS) {
            if (lowerFilename.endsWith(disallowed)) {
                throw new AuthException("ATTACHMENT_TYPE_NOT_SUPPORTED", HttpStatus.BAD_REQUEST,
                        "Định dạng tệp không được hỗ trợ. Chỉ chấp nhận PDF, DOCX hoặc TXT.");
            }
        }

        boolean hasAllowedExtension = false;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lowerFilename.endsWith(ext)) {
                hasAllowedExtension = true;
                break;
            }
        }

        if (!hasAllowedExtension) {
            throw new AuthException("ATTACHMENT_TYPE_NOT_SUPPORTED", HttpStatus.BAD_REQUEST,
                    "Định dạng tệp không được hỗ trợ. Chỉ chấp nhận PDF, DOCX hoặc TXT.");
        }

        String contentType = file.getContentType();
        if (contentType != null) {
            String lowerContentType = contentType.toLowerCase(Locale.ROOT);
            for (String disallowedMime : DISALLOWED_MIME_PREFIXES) {
                if (lowerContentType.startsWith(disallowedMime)) {
                    throw new AuthException("ATTACHMENT_TYPE_NOT_SUPPORTED", HttpStatus.BAD_REQUEST,
                            "Định dạng tệp không được hỗ trợ. Chỉ chấp nhận PDF, DOCX hoặc TXT.");
                }
            }
        }

        Map<UUID, TutorAttachment> userMap = store.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
        boolean hasActive = userMap.values().stream().anyMatch(this::isActive);
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
                AttachmentStatus.READY,
                null,
                now,
                now
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
                || attachment.status() == AttachmentStatus.READY;
    }
}
