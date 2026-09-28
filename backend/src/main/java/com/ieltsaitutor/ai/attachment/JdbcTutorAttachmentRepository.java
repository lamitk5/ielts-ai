package com.ieltsaitutor.ai.attachment;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTutorAttachmentRepository implements TutorAttachmentRepository {
    private static final RowMapper<TutorAttachment> MAPPER = (rs, row) -> new TutorAttachment(
            (UUID) rs.getObject("id"), (UUID) rs.getObject("owner_user_id"), (UUID) rs.getObject("conversation_id"),
            rs.getString("original_filename"), rs.getString("sanitized_filename"), rs.getString("media_type"),
            AttachmentKind.valueOf(rs.getString("attachment_kind")), rs.getLong("size_bytes"), rs.getString("sha256"),
            rs.getString("storage_key"), TutorAttachment.AttachmentStatus.valueOf(rs.getString("status")),
            rs.getString("processing_error_code"), instant(rs.getTimestamp("processing_started_at")),
            rs.getInt("processing_attempts"), instant(rs.getTimestamp("created_at")), instant(rs.getTimestamp("updated_at")),
            instant(rs.getTimestamp("expires_at")));

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcTutorAttachmentRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void save(TutorAttachment attachment) {
        jdbc.update("""
                INSERT INTO ai_attachments
                (id, owner_user_id, conversation_id, original_filename, sanitized_filename, media_type, attachment_kind,
                 size_bytes, sha256, storage_key, status, processing_error_code, processing_started_at, processing_attempts,
                 created_at, updated_at, expires_at)
                VALUES (:id, :ownerUserId, :conversationId, :originalFilename, :sanitizedFilename, :mediaType, :attachmentKind,
                        :sizeBytes, :sha256, :storageKey, :status, :processingErrorCode, :processingStartedAt, :processingAttempts,
                        :createdAt, :updatedAt, :expiresAt)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, processing_error_code = EXCLUDED.processing_error_code,
                    processing_started_at = EXCLUDED.processing_started_at, processing_attempts = EXCLUDED.processing_attempts,
                    updated_at = EXCLUDED.updated_at
                """, params(attachment));
    }

    @Override
    public Optional<TutorAttachment> findOwned(UUID userId, UUID conversationId, UUID attachmentId) {
        return jdbc.query("SELECT * FROM ai_attachments WHERE owner_user_id=:userId AND conversation_id=:conversationId AND id=:id",
                new MapSqlParameterSource().addValue("userId", userId).addValue("conversationId", conversationId).addValue("id", attachmentId), MAPPER).stream().findFirst();
    }

    @Override
    public List<TutorAttachment> findOwnedByIds(UUID userId, UUID conversationId, List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return jdbc.query("SELECT * FROM ai_attachments WHERE owner_user_id=:userId AND conversation_id=:conversationId AND id IN (:ids)",
                new MapSqlParameterSource().addValue("userId", userId).addValue("conversationId", conversationId).addValue("ids", ids), MAPPER);
    }

    @Override
    public int updateStatus(UUID attachmentId, TutorAttachment.AttachmentStatus status, String errorCode) {
        return jdbc.update("UPDATE ai_attachments SET status=:status, processing_error_code=:errorCode, updated_at=CURRENT_TIMESTAMP WHERE id=:id",
                new MapSqlParameterSource().addValue("id", attachmentId).addValue("status", status.name()).addValue("errorCode", errorCode));
    }

    @Override
    public List<TutorAttachment> findStaleProcessing(Instant cutoff) {
        return jdbc.query("SELECT * FROM ai_attachments WHERE status='PROCESSING' AND processing_started_at < :cutoff",
                new MapSqlParameterSource("cutoff", Timestamp.from(cutoff)), MAPPER);
    }

    private static MapSqlParameterSource params(TutorAttachment a) {
        return new MapSqlParameterSource().addValue("id", a.id()).addValue("ownerUserId", a.userId())
                .addValue("conversationId", a.conversationId()).addValue("originalFilename", a.filename())
                .addValue("sanitizedFilename", a.sanitizedFilename()).addValue("mediaType", a.contentType())
                .addValue("attachmentKind", a.kind().name()).addValue("sizeBytes", a.sizeBytes()).addValue("sha256", a.sha256())
                .addValue("storageKey", a.storageKey()).addValue("status", a.status().name())
                .addValue("processingErrorCode", a.processingErrorCode()).addValue("processingStartedAt", timestamp(a.processingStartedAt()))
                .addValue("processingAttempts", a.processingAttempts()).addValue("createdAt", timestamp(a.createdAt()))
                .addValue("updatedAt", timestamp(a.updatedAt())).addValue("expiresAt", timestamp(a.expiresAt()));
    }

    private static Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }
    private static Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
}
