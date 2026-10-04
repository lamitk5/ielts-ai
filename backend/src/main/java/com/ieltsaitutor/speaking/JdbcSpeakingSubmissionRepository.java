package com.ieltsaitutor.speaking;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSpeakingSubmissionRepository implements SpeakingSubmissionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSpeakingSubmissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<SpeakingSubmission> ROW_MAPPER = (rs, rowNum) -> {
        UUID id = (UUID) rs.getObject("id");
        UUID submissionId = (UUID) rs.getObject("submission_id");
        String promptId = rs.getString("prompt_id");
        String promptVersion = rs.getString("prompt_version");
        int prep = rs.getInt("preparation_seconds");
        int resp = rs.getInt("response_seconds");
        String audioKey = rs.getString("audio_storage_key");
        String audioMime = rs.getString("audio_mime_type");
        Long audioSize = rs.getObject("audio_size_bytes") != null ? rs.getLong("audio_size_bytes") : null;
        String transcript = rs.getString("transcript");
        String transcriptSource = rs.getString("transcript_source");
        SpeakingSubmissionState status = SpeakingSubmissionState.valueOf(rs.getString("status"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        return new SpeakingSubmission(
                id, submissionId, promptId, promptVersion, prep, resp,
                audioKey, audioMime, audioSize, transcript, transcriptSource,
                status, createdAt.toInstant(), updatedAt.toInstant());
    };

    @Override
    public SpeakingSubmission save(SpeakingSubmission sub) {
        String sql = """
                INSERT INTO speaking_submissions
                (id, submission_id, prompt_id, prompt_version, preparation_seconds, response_seconds,
                 audio_storage_key, audio_mime_type, audio_size_bytes, transcript, transcript_source,
                 status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                  preparation_seconds = EXCLUDED.preparation_seconds,
                  response_seconds = EXCLUDED.response_seconds,
                  audio_storage_key = EXCLUDED.audio_storage_key,
                  audio_mime_type = EXCLUDED.audio_mime_type,
                  audio_size_bytes = EXCLUDED.audio_size_bytes,
                  transcript = EXCLUDED.transcript,
                  transcript_source = EXCLUDED.transcript_source,
                  status = EXCLUDED.status,
                  updated_at = EXCLUDED.updated_at
                """;
        jdbcTemplate.update(sql,
                sub.id(),
                sub.submissionId(),
                sub.promptId(),
                sub.promptVersion(),
                sub.preparationSeconds(),
                sub.responseSeconds(),
                sub.audioStorageKey(),
                sub.audioMimeType(),
                sub.audioSizeBytes(),
                sub.transcript(),
                sub.transcriptSource(),
                sub.status().name(),
                Timestamp.from(sub.createdAt()),
                Timestamp.from(sub.updatedAt()));
        return sub;
    }

    @Override
    public Optional<SpeakingSubmission> findById(UUID id) {
        String sql = "SELECT * FROM speaking_submissions WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<SpeakingSubmission> findBySubmissionId(UUID submissionId) {
        String sql = "SELECT * FROM speaking_submissions WHERE submission_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, ROW_MAPPER, submissionId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public SpeakingSubmission updateStatus(UUID submissionId, SpeakingSubmissionState status) {
        String sql = "UPDATE speaking_submissions SET status = ?, updated_at = ? WHERE submission_id = ?";
        Instant now = Instant.now();
        jdbcTemplate.update(sql, status.name(), Timestamp.from(now), submissionId);
        return findBySubmissionId(submissionId).orElseThrow();
    }
}
