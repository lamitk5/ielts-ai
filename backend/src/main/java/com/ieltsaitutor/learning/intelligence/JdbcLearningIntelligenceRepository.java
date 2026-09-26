package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JdbcLearningIntelligenceRepository implements LearningIntelligenceRepository, RoadmapRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    public JdbcLearningIntelligenceRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<LearningEvent> findEvents(UUID userId) {
        return jdbc.query("SELECT * FROM learning_events WHERE user_id=:userId ORDER BY recorded_at DESC",
                new MapSqlParameterSource("userId", userId), (rs, row) -> new LearningEvent(
                        UUID.fromString(rs.getString("id")), UUID.fromString(rs.getString("user_id")),
                        LearningEventType.valueOf(rs.getString("event_type")), Skill.valueOf(rs.getString("skill")),
                        (UUID) rs.getObject("session_id"), rs.getString("practice_set_id"), (UUID) rs.getObject("attempt_id"),
                        rs.getString("question_id"), (UUID) rs.getObject("roadmap_item_id"), rs.getString("source_reference"),
                        payload(rs.getString("payload")), rs.getString("client_event_id"),
                        rs.getTimestamp("occurred_at").toInstant(), rs.getTimestamp("recorded_at").toInstant()));
    }

    @Override
    public List<MistakeRecord> findMistakes(UUID userId) {
        return jdbc.query("SELECT * FROM learning_mistakes WHERE user_id=:userId ORDER BY detected_at DESC",
                new MapSqlParameterSource("userId", userId), (rs, row) -> new MistakeRecord(
                        (UUID) rs.getObject("id"), (UUID) rs.getObject("user_id"), Skill.valueOf(rs.getString("skill")),
                        rs.getString("practice_set_id"), (UUID) rs.getObject("attempt_id"), rs.getString("question_id"),
                        rs.getString("question_type"), rs.getString("learner_answer_snapshot"), rs.getString("correct_answer_ref"),
                        rs.getString("category"), MistakeMethod.valueOf(rs.getString("method")), rs.getDouble("confidence"),
                        rs.getString("evidence_code"), rs.getString("evidence_text"), rs.getTimestamp("detected_at").toInstant(),
                        rs.getTimestamp("resolved_at") == null ? null : rs.getTimestamp("resolved_at").toInstant(),
                        MistakeStatus.valueOf(rs.getString("status")), rs.getInt("classification_revision")));
    }

    @Override
    public Optional<LearningRoadmapItem> findItem(UUID userId, UUID itemId) {
        return jdbc.query("SELECT * FROM learning_roadmap_items WHERE user_id=:userId AND id=:itemId",
                new MapSqlParameterSource("userId", userId).addValue("itemId", itemId), (rs, row) -> mapItem(rs))
                .stream().findFirst();
    }

    @Override
    public LearningRoadmapItem markCompleted(UUID userId, UUID itemId) {
        jdbc.update("UPDATE learning_roadmap_items SET status='COMPLETED' WHERE user_id=:userId AND id=:itemId",
                new MapSqlParameterSource("userId", userId).addValue("itemId", itemId));
        return findItem(userId, itemId).orElseThrow();
    }

    private LearningRoadmapItem mapItem(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new LearningRoadmapItem((UUID) rs.getObject("id"), (UUID) rs.getObject("roadmap_id"),
                (UUID) rs.getObject("user_id"), Skill.valueOf(rs.getString("skill")), rs.getString("learning_objective"),
                rs.getString("activity_type"), rs.getString("target_error_type"), rs.getString("target_question_type"),
                rs.getInt("priority"), rs.getString("estimated_workload"), RoadmapItemStatus.valueOf(rs.getString("status")),
                rs.getString("reason_code"), (UUID) rs.getObject("evidence_issue_id"), payload(rs.getString("evidence_snapshot")));
    }

    private java.util.Map<String, Object> payload(String value) {
        try { return value == null ? java.util.Map.of() : mapper.readValue(value, java.util.Map.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("stored learning payload is invalid", e); }
    }
}
