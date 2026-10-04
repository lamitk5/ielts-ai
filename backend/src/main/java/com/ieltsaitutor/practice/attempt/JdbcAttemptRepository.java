package com.ieltsaitutor.practice.attempt;

import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JdbcAttemptRepository implements AttemptRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final RowMapper<PracticeAttempt> MAPPER = (rs, row) -> new PracticeAttempt(
            rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class), rs.getString("set_id"),
            rs.getString("practice_version_id"), rs.getString("skill"), parseStatus(rs.getString("attempt_status")),
            parseAnswers(rs.getString("answer_payload")), (Integer) rs.getObject("score"), (Integer) rs.getObject("total"),
            rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("submitted_at") == null ? null : rs.getTimestamp("submitted_at").toInstant(),
            rs.getString("result_payload"), rs.getString("idempotency_key"));

    public JdbcAttemptRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public PracticeAttempt create(UUID userId, String practiceId, String practiceVersion, String skill, String idempotencyKey) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO learning_attempts(id,user_id,skill,set_id,score,total,answer_payload,created_at,practice_version_id,attempt_status,idempotency_key)
                VALUES(:id,:userId,:skill,:setId,0,1,'{}'::jsonb,:createdAt,:practiceVersion,'IN_PROGRESS',:idempotencyKey)
                """, new MapSqlParameterSource().addValue("id", id).addValue("userId", userId)
                .addValue("skill", skill.toUpperCase()).addValue("setId", practiceId)
                .addValue("createdAt", now.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("practiceVersion", practiceVersion).addValue("idempotencyKey", idempotencyKey));
        return new PracticeAttempt(id, userId, practiceId, practiceVersion, skill, AttemptStatus.IN_PROGRESS, Map.of(), null, null, now, null, "{}", idempotencyKey);
    }

    @Override public Optional<PracticeAttempt> findById(UUID id) { return query("WHERE id = :id", new MapSqlParameterSource("id", id)).stream().findFirst(); }

    @Override public Optional<PracticeAttempt> findByUserAndIdempotencyKey(UUID userId, String key) {
        return query("WHERE user_id = :userId AND idempotency_key = :idempotencyKey", new MapSqlParameterSource().addValue("userId", userId).addValue("idempotencyKey", key)).stream().findFirst();
    }

    @Override public PracticeAttempt saveAnswers(PracticeAttempt attempt, Map<String, String> answers) {
        jdbc.update("UPDATE learning_attempts SET answer_payload = CAST(:answers AS jsonb) WHERE id = :id AND attempt_status = 'IN_PROGRESS'",
                new MapSqlParameterSource().addValue("id", attempt.id()).addValue("answers", json(answers)));
        return attempt.withAnswers(answers);
    }

    @Override public PracticeAttempt saveSubmitted(PracticeAttempt attempt, Map<String, String> answers) {
        Instant submitted = Instant.now();
        jdbc.update("""
                UPDATE learning_attempts SET answer_payload = CAST(:answers AS jsonb),
                    result_payload = '{}'::jsonb, attempt_status = 'SUBMITTED', submitted_at = :submittedAt
                WHERE id = :id AND attempt_status = 'IN_PROGRESS'
                """, new MapSqlParameterSource().addValue("id", attempt.id()).addValue("answers", json(answers))
                .addValue("submittedAt", submitted.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE));
        return attempt.withSubmitted(answers);
    }

    @Override public PracticeAttempt saveResult(PracticeAttempt attempt, Map<String, String> answers, int score, int total, String resultPayload) {
        Instant submitted = Instant.now();
        jdbc.update("""
                UPDATE learning_attempts SET answer_payload = CAST(:answers AS jsonb), score = :score, total = :total,
                    result_payload = CAST(:resultPayload AS jsonb), attempt_status = 'FEEDBACK_READY', submitted_at = :submittedAt
                WHERE id = :id AND attempt_status = 'IN_PROGRESS'
                """, new MapSqlParameterSource().addValue("id", attempt.id()).addValue("answers", json(answers))
                .addValue("score", score).addValue("total", total).addValue("resultPayload", resultPayload == null ? "{}" : resultPayload)
                .addValue("submittedAt", submitted.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE));
        return attempt.withResult(answers, score, total, resultPayload);
    }

    private List<PracticeAttempt> query(String where, MapSqlParameterSource params) {
        return jdbc.query("SELECT id,user_id,skill,set_id,practice_version_id,attempt_status,score,total,answer_payload,created_at,submitted_at,result_payload,idempotency_key FROM learning_attempts " + where,
                params, MAPPER);
    }

    private String json(Object value) { try { return mapper.writeValueAsString(value); } catch (Exception error) { throw new IllegalArgumentException("Invalid attempt payload", error); } }
    private static Map<String, String> parseAnswers(String value) { try { return new ObjectMapper().readValue(value == null ? "{}" : value, new TypeReference<>() {}); } catch (Exception ignored) { return Map.of(); } }
    private static AttemptStatus parseStatus(String value) { try { return AttemptStatus.valueOf(value); } catch (Exception ignored) { return AttemptStatus.SUBMITTED; } }
}
