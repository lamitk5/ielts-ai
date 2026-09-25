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
    public java.util.Optional<SpeakingAttempt> findByUserAndId(UUID userId, UUID attemptId) {
        return jdbc.query("""
                SELECT * FROM speaking_attempts WHERE user_id=:userId AND id=:attemptId
                """, new MapSqlParameterSource().addValue("userId", userId).addValue("attemptId", attemptId),
                (rs, row) -> new SpeakingAttempt(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                        rs.getString("prompt_id"), rs.getString("transcript"), rs.getString("audio_filename"),
                        rs.getString("attempt_status"), (Double) rs.getObject("overall_band_estimate"),
                        rs.getTimestamp("created_at").toInstant())).stream().findFirst();
    }
}
