package com.ieltsaitutor.practice;

import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JdbcPracticeAttemptStore implements PracticeAttemptStore {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JdbcPracticeAttemptStore(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void save(UUID userId, String skill, String setId, int score, int total, Map<String, String> answers) {
        saveAndReturn(userId, skill, setId, score, total, answers);
    }

    @Override
    public UUID saveAndReturn(UUID userId, String skill, String setId, int score, int total, Map<String, String> answers) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String payload = answers.entrySet().stream().map(entry -> "\"" + escape(entry.getKey()) + "\":\"" + escape(entry.getValue()) + "\"")
                .collect(Collectors.joining(",", "{", "}"));
        jdbc.update("""
                INSERT INTO learning_attempts(id,user_id,skill,set_id,score,total,answer_payload,created_at)
                VALUES(:id,:userId,:skill,:setId,:score,:total,CAST(:payload AS jsonb),:createdAt)
                """, new MapSqlParameterSource().addValue("id", id).addValue("userId", userId)
                .addValue("skill", skill.toUpperCase()).addValue("setId", setId).addValue("score", score)
                .addValue("total", total).addValue("payload", payload)
                .addValue("createdAt", now.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE));
        jdbc.update("""
                INSERT INTO learning_activity(id,user_id,skill,activity_type,reference_id,score,created_at)
                VALUES(:id,:userId,:skill,'PRACTICE_COMPLETED',:referenceId,:score,:createdAt)
                """, new MapSqlParameterSource().addValue("id", UUID.randomUUID()).addValue("userId", userId)
                .addValue("skill", skill.toUpperCase()).addValue("referenceId", setId).addValue("score", score * 9d / total)
                .addValue("createdAt", now.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE));
        return id;
    }

    @Override
    public java.util.Optional<PracticeAttemptSnapshot> findLatest(UUID userId, String skill, String setId) {
        return jdbc.query("""
                SELECT id, user_id, skill, set_id, score, total, answer_payload, created_at
                FROM learning_attempts WHERE user_id = :userId AND skill = :skill AND set_id = :setId
                ORDER BY created_at DESC LIMIT 1
                """, new MapSqlParameterSource().addValue("userId", userId)
                .addValue("skill", skill == null ? null : skill.toUpperCase()).addValue("setId", setId),
                (rs, row) -> new PracticeAttemptSnapshot(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                        rs.getString("skill"), rs.getString("set_id"), rs.getInt("score"), rs.getInt("total"),
                        parseAnswers(rs.getString("answer_payload")), rs.getTimestamp("created_at").toInstant()))
                .stream().findFirst();
    }

    private Map<String, String> parseAnswers(String payload) {
        try { return objectMapper.readValue(payload == null ? "{}" : payload, new TypeReference<>() {}); }
        catch (Exception ignored) { return Map.of(); }
    }

    private String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
