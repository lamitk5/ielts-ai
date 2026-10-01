package com.ieltsaitutor.submission;

import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPracticeSubmissionRepository implements PracticeSubmissionRepository {
    private final NamedParameterJdbcTemplate jdbc;

    private static final RowMapper<PracticeSubmission> MAPPER = (rs, row) -> new PracticeSubmission(
            rs.getObject("id", UUID.class),
            rs.getObject("user_id", UUID.class),
            rs.getString("skill"),
            rs.getString("practice_id"),
            rs.getString("practice_version_id"),
            rs.getString("published_set_id"),
            rs.getInt("publication_revision"),
            SubmissionStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("started_at").toInstant(),
            rs.getTimestamp("last_saved_at") == null ? null : rs.getTimestamp("last_saved_at").toInstant(),
            rs.getTimestamp("submitted_at") == null ? null : rs.getTimestamp("submitted_at").toInstant(),
            rs.getTimestamp("scored_at") == null ? null : rs.getTimestamp("scored_at").toInstant(),
            rs.getLong("autosave_revision"),
            rs.getString("start_idempotency_key"),
            rs.getString("submit_idempotency_key"),
            rs.getString("content_hash"),
            rs.getBoolean("retryable"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());

    public JdbcPracticeSubmissionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PracticeSubmission create(PracticeSubmission submission) {
        jdbc.update("""
                INSERT INTO practice_submissions(
                    id,user_id,skill,practice_id,practice_version_id,published_set_id,
                    publication_revision,status,started_at,last_saved_at,submitted_at,
                    scored_at,autosave_revision,start_idempotency_key,submit_idempotency_key,
                    content_hash,retryable,created_at,updated_at)
                VALUES(:id,:userId,:skill,:practiceId,:practiceVersionId,:publishedSetId,
                    :publicationRevision,:status,:startedAt,:lastSavedAt,:submittedAt,
                    :scoredAt,:autosaveRevision,:startKey,:submitKey,:contentHash,
                    :retryable,:createdAt,:updatedAt)
                """, params(submission));
        return submission;
    }

    @Override
    public Optional<PracticeSubmission> findById(UUID id) {
        return query("WHERE id = :id", new MapSqlParameterSource("id", id)).stream().findFirst();
    }

    @Override
    public Optional<PracticeSubmission> findByOwnerAndId(UUID ownerId, UUID id) {
        return query("WHERE id = :id AND user_id = :userId",
                new MapSqlParameterSource().addValue("id", id).addValue("userId", ownerId)).stream().findFirst();
    }

    @Override
    public Optional<PracticeSubmission> findByOwnerAndStartIdempotencyKey(UUID ownerId, String key) {
        return query("WHERE user_id = :userId AND start_idempotency_key = :key",
                new MapSqlParameterSource().addValue("userId", ownerId).addValue("key", key)).stream().findFirst();
    }

    @Override
    public Optional<PracticeSubmission> findByOwnerAndSubmitIdempotencyKey(UUID ownerId, String key) {
        return query("WHERE user_id = :userId AND submit_idempotency_key = :key",
                new MapSqlParameterSource().addValue("userId", ownerId).addValue("key", key)).stream().findFirst();
    }

    @Override
    public PracticeSubmission updateStatus(UUID id, SubmissionStatus status, Instant updatedAt) {
        jdbc.update("UPDATE practice_submissions SET status = :status, updated_at = :updatedAt WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("status", status.name())
                        .addValue("updatedAt", updatedAt, Types.TIMESTAMP_WITH_TIMEZONE));
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Submission not found"));
    }

    @Override
    public Optional<PracticeSubmission> updateAutosaveIfRevision(UUID id, long expectedRevision, Instant savedAt) {
        int updated = jdbc.update("""
                UPDATE practice_submissions
                SET autosave_revision = autosave_revision + 1, last_saved_at = :savedAt, updated_at = :savedAt
                WHERE id = :id AND autosave_revision = :expectedRevision
                    AND status IN ('DRAFT', 'IN_PROGRESS')
                """, new MapSqlParameterSource().addValue("id", id)
                .addValue("expectedRevision", expectedRevision).addValue("savedAt", savedAt, Types.TIMESTAMP_WITH_TIMEZONE));
        return updated == 0 ? Optional.empty() : findById(id);
    }

    @Override
    public Optional<PracticeSubmission> finalizeIfEditable(UUID id, String submitIdempotencyKey,
            String contentHash, Instant submittedAt) {
        int updated = jdbc.update("""
                UPDATE practice_submissions
                SET status = 'SUBMITTED', submit_idempotency_key = :submitKey, content_hash = :contentHash,
                    submitted_at = :submittedAt,
                    duration_seconds = GREATEST(0, EXTRACT(EPOCH FROM (:submittedAt - started_at))::BIGINT),
                    updated_at = :submittedAt
                WHERE id = :id AND status IN ('DRAFT', 'IN_PROGRESS')
                """, new MapSqlParameterSource().addValue("id", id)
                .addValue("submitKey", submitIdempotencyKey).addValue("contentHash", contentHash)
                .addValue("submittedAt", submittedAt, Types.TIMESTAMP_WITH_TIMEZONE));
        return updated == 0 ? Optional.empty() : findById(id);
    }

    @Override
    public SubmissionHistoryPage findHistory(UUID ownerId, String skill, SubmissionStatus status, int page, int size) {
        String filters = "WHERE user_id = :userId "
                + "AND (:skill IS NULL OR lower(skill) = lower(:skill)) "
                + "AND (:status IS NULL OR status = :status)";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", ownerId)
                .addValue("skill", skill).addValue("status", status == null ? null : status.name())
                .addValue("limit", size).addValue("offset", (long) page * size);
        List<PracticeSubmission> items = jdbc.query("SELECT id,user_id,skill,practice_id,practice_version_id,published_set_id,"
                        + "publication_revision,status,started_at,last_saved_at,submitted_at,scored_at,autosave_revision,"
                        + "start_idempotency_key,submit_idempotency_key,content_hash,retryable,created_at,updated_at "
                        + "FROM practice_submissions " + filters + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset",
                params, MAPPER);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM practice_submissions " + filters, params, Long.class);
        return new SubmissionHistoryPage(items, page, size, total == null ? 0 : total);
    }

    private List<PracticeSubmission> query(String where, MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT id,user_id,skill,practice_id,practice_version_id,published_set_id,
                    publication_revision,status,started_at,last_saved_at,submitted_at,scored_at,
                    autosave_revision,start_idempotency_key,submit_idempotency_key,content_hash,
                    retryable,created_at,updated_at
                FROM practice_submissions """ + where, params, MAPPER);
    }

    private MapSqlParameterSource params(PracticeSubmission item) {
        return new MapSqlParameterSource()
                .addValue("id", item.id())
                .addValue("userId", item.userId())
                .addValue("skill", item.skill())
                .addValue("practiceId", item.practiceId())
                .addValue("practiceVersionId", item.practiceVersionId())
                .addValue("publishedSetId", item.publishedSetId())
                .addValue("publicationRevision", item.publicationRevision())
                .addValue("status", item.status().name())
                .addValue("startedAt", item.startedAt(), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("lastSavedAt", item.lastSavedAt(), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("submittedAt", item.submittedAt(), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("scoredAt", item.scoredAt(), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("autosaveRevision", item.autosaveRevision())
                .addValue("startKey", item.startIdempotencyKey())
                .addValue("submitKey", item.submitIdempotencyKey())
                .addValue("contentHash", item.contentHash())
                .addValue("retryable", item.retryable())
                .addValue("createdAt", item.createdAt(), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("updatedAt", item.updatedAt(), Types.TIMESTAMP_WITH_TIMEZONE);
    }
}
