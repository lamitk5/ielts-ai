package com.ieltsaitutor.admin.portal;

import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import com.ieltsaitutor.submission.SubmissionOwnershipService;

@Service
public class SubmissionReportService {
    private static final java.util.Set<String> CATEGORIES = java.util.Set.of("CONTENT_ERROR", "SCORING_CONCERN", "TECHNICAL_ISSUE", "OTHER");
    private final NamedParameterJdbcTemplate jdbc;
    private final PracticeSubmissionRepository submissions;
    private final SubmissionOwnershipService ownership;
    private final AdminAuditService audit;

    public SubmissionReportService(NamedParameterJdbcTemplate jdbc, PracticeSubmissionRepository submissions,
            SubmissionOwnershipService ownership, AdminAuditService audit) {
        this.jdbc = jdbc; this.submissions = submissions; this.ownership = ownership; this.audit = audit;
    }

    public Report create(UUID ownerId, UUID submissionId, String category, String comment) {
        var submission = submissions.findById(submissionId).orElseThrow(() -> new IllegalArgumentException("Bài nộp không tồn tại."));
        ownership.requireOwner(ownerId, submission);
        String normalized = category == null ? "OTHER" : category.trim().toUpperCase(java.util.Locale.ROOT);
        if (!CATEGORIES.contains(normalized)) throw new IllegalArgumentException("Loại báo cáo không hợp lệ.");
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("INSERT INTO submission_reports(id,submission_id,owner_user_id,category,comment,status,created_at) "
                    + "VALUES(:id,:submission,:owner,:category,:comment,'OPEN',:createdAt)",
                    new MapSqlParameterSource().addValue("id", id).addValue("submission", submissionId).addValue("owner", ownerId)
                            .addValue("category", normalized).addValue("comment", comment == null ? "" : comment.trim())
                            .addValue("createdAt", Instant.now()));
        } catch (DuplicateKeyException duplicate) {
            throw new IllegalStateException("Báo cáo tương tự đang được xử lý.");
        }
        audit.record(ownerId, "SUBMISSION_REPORTED", "submission", submissionId.toString(), "{\"category\":\"" + normalized + "\"}");
        return new Report(id, submissionId, normalized, comment == null ? "" : comment.trim(), "OPEN");
    }

    public record Report(UUID id, UUID submissionId, String category, String comment, String status) {}
}
