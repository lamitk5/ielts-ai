package com.ieltsaitutor.learning;

import java.sql.Types;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLearningRepository implements LearningRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcLearningRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void saveAttempt(LearningAttempt attempt) {
        jdbc.update("""
                INSERT INTO learning_attempts(id,user_id,skill,set_id,score,total,answer_payload,created_at)
                VALUES(:id,:userId,:skill,:setId,:score,:total,CAST(:payload AS jsonb),:createdAt)
                """, new MapSqlParameterSource().addValue("id", attempt.id()).addValue("userId", attempt.userId())
                .addValue("skill", attempt.skill().toUpperCase()).addValue("setId", "attempt-" + attempt.id())
                .addValue("score", attempt.score()).addValue("total", attempt.total()).addValue("payload", "{}")
                .addValue("createdAt", attempt.createdAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE));
    }

    @Override
    public List<LearningAttempt> findAttempts(UUID userId) {
        return jdbc.query("SELECT * FROM learning_attempts WHERE user_id=:userId ORDER BY created_at DESC",
                new MapSqlParameterSource("userId", userId), (rs, row) -> new LearningAttempt(
                        rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                        rs.getString("skill").toLowerCase(), rs.getInt("score"), rs.getInt("total"),
                        rs.getTimestamp("created_at").toInstant()));
    }

    @Override
    public void saveActivity(LearningActivity activity) {
        jdbc.update("""
                INSERT INTO learning_activity(id,user_id,skill,activity_type,reference_id,score,created_at)
                VALUES(:id,:userId,:skill,:type,:referenceId,:score,:createdAt)
                """, new MapSqlParameterSource().addValue("id", activity.id()).addValue("userId", activity.userId())
                .addValue("skill", activity.skill().toUpperCase()).addValue("type", activity.activityType())
                .addValue("referenceId", activity.referenceId()).addValue("score", activity.score())
                .addValue("createdAt", activity.createdAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE));
    }

    @Override
    public List<LearningActivity> findActivities(UUID userId) {
        return jdbc.query("SELECT * FROM learning_activity WHERE user_id=:userId ORDER BY created_at DESC LIMIT 20",
                new MapSqlParameterSource("userId", userId), (rs, row) -> new LearningActivity(
                        rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                        rs.getString("skill").toLowerCase(), rs.getString("activity_type"),
                        rs.getString("reference_id"), (Double) rs.getObject("score"), rs.getTimestamp("created_at").toInstant()));
    }
}
