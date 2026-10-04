package com.ieltsaitutor.speaking;

import java.sql.Types;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSpeakingRepository implements SpeakingRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcSpeakingRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void save(SpeakingAttempt attempt) {
        jdbc.update("""
                INSERT INTO speaking_attempts(id,user_id,prompt_id,transcript,audio_filename,attempt_status,overall_band_estimate,created_at)
                VALUES(:id,:userId,:promptId,:transcript,:audioFilename,:status,:estimate,:createdAt)
                """, new MapSqlParameterSource().addValue("id", attempt.id()).addValue("userId", attempt.userId())
                .addValue("promptId", attempt.promptId()).addValue("transcript", attempt.transcript())
                .addValue("audioFilename", attempt.audioFilename()).addValue("status", attempt.status())
                .addValue("estimate", attempt.overallBandEstimate()).addValue("createdAt", attempt.createdAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE));
    }

    @Override
    public List<SpeakingAttempt> findByUser(UUID userId) {
        return jdbc.query("SELECT * FROM speaking_attempts WHERE user_id=:userId ORDER BY created_at DESC",
                new MapSqlParameterSource("userId", userId), (rs, row) -> new SpeakingAttempt(
                rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class), rs.getString("prompt_id"),
                        rs.getString("transcript"), rs.getString("audio_filename"), rs.getString("attempt_status"),
                        (Double) rs.getObject("overall_band_estimate"), rs.getTimestamp("created_at").toInstant()));
    }

    @Override
    public SpeakingAttempt start(UUID userId, String promptId) {
        java.util.Optional<SpeakingAttempt> active = jdbc.query("""
                SELECT id,user_id,prompt_id,transcript,audio_filename,attempt_status,overall_band_estimate,created_at
                FROM speaking_attempts WHERE user_id=:userId AND prompt_id=:promptId AND attempt_status IN ('IN_PROGRESS','DRAFT')
                ORDER BY created_at DESC LIMIT 1
                """, new MapSqlParameterSource().addValue("userId", userId).addValue("promptId", promptId), this::map).stream().findFirst();
        if (active.isPresent()) return active.get();
        UUID id = UUID.randomUUID();
        java.time.Instant created = java.time.Instant.now();
        jdbc.update("""
                INSERT INTO speaking_attempts(id,user_id,prompt_id,transcript,audio_filename,attempt_status,overall_band_estimate,created_at,submitted_at)
                VALUES(:id,:userId,:promptId,NULL,NULL,'IN_PROGRESS',NULL,:createdAt,NULL)
                """, new MapSqlParameterSource().addValue("id", id).addValue("userId", userId).addValue("promptId", promptId)
                .addValue("createdAt", created.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE));
        return new SpeakingAttempt(id, userId, promptId, null, null, "IN_PROGRESS", null, created);
    }

    @Override
    public SpeakingAttempt saveDraft(SpeakingAttempt attempt) {
        jdbc.update("UPDATE speaking_attempts SET transcript=:transcript,attempt_status=:status WHERE id=:id AND attempt_status IN ('IN_PROGRESS','DRAFT')",
                new MapSqlParameterSource().addValue("id", attempt.id()).addValue("transcript", attempt.transcript()).addValue("status", attempt.status()));
        return attempt;
    }

    @Override
    public SpeakingAttempt complete(SpeakingAttempt attempt, SpeakingAttempt result) {
        java.time.Instant submitted = java.time.Instant.now();
        jdbc.update("""
                UPDATE speaking_attempts SET transcript=:transcript,audio_filename=:audioFilename,attempt_status=:status,
                    overall_band_estimate=NULL,submitted_at=:submittedAt
                WHERE id=:id AND attempt_status IN ('IN_PROGRESS','DRAFT')
                """, new MapSqlParameterSource().addValue("id", attempt.id()).addValue("transcript", result.transcript())
                .addValue("audioFilename", result.audioFilename()).addValue("status", result.status())
                .addValue("submittedAt", submitted.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE));
        return result;
    }

    @Override
    public java.util.Optional<SpeakingAttempt> findByUserAndId(UUID userId, UUID attemptId) {
        return jdbc.query("""
                SELECT * FROM speaking_attempts WHERE user_id=:userId AND id=:attemptId
                """, new MapSqlParameterSource().addValue("userId", userId).addValue("attemptId", attemptId),
                (rs, row) -> new SpeakingAttempt(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                        rs.getString("prompt_id"), rs.getString("transcript"), rs.getString("audio_filename"),
                        rs.getString("attempt_status"), (Double) rs.getObject("overall_band_estimate"),
                        rs.getTimestamp("created_at").toInstant())).stream().findFirst();
    }

    private SpeakingAttempt map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new SpeakingAttempt(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class), rs.getString("prompt_id"),
                rs.getString("transcript"), rs.getString("audio_filename"), rs.getString("attempt_status"),
                (Double) rs.getObject("overall_band_estimate"), rs.getTimestamp("created_at").toInstant());
    }
}
