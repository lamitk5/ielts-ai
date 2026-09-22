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

@Repository
public class JdbcPracticeAttemptStore implements PracticeAttemptStore {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPracticeAttemptStore(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void save(UUID userId, String skill, String setId, int score, int total, Map<String, String> answers) {
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
    }

    private String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
