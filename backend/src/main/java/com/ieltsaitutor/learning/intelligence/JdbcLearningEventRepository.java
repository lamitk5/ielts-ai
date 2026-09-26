package com.ieltsaitutor.learning.intelligence;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JdbcLearningEventRepository implements LearningEventRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    public JdbcLearningEventRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<LearningEvent> findByClientEvent(UUID userId, String clientEventId) {
        return find("user_id = :userId AND client_event_id = :value", userId, clientEventId);
    }

    @Override
    public Optional<LearningEvent> findBySourceReference(UUID userId, String sourceReference) {
        return find("user_id = :userId AND source_reference = :value", userId, sourceReference);
    }

    private Optional<LearningEvent> find(String predicate, UUID userId, String value) {
        return jdbc.query("SELECT * FROM learning_events WHERE " + predicate + " LIMIT 1",
                new MapSqlParameterSource("userId", userId).addValue("value", value),
                (rs, row) -> new LearningEvent(UUID.fromString(rs.getString("id")), UUID.fromString(rs.getString("user_id")),
                        LearningEventType.valueOf(rs.getString("event_type")), Skill.valueOf(rs.getString("skill")),
                        (UUID) rs.getObject("session_id"), rs.getString("practice_set_id"), (UUID) rs.getObject("attempt_id"),
                        rs.getString("question_id"), (UUID) rs.getObject("roadmap_item_id"), rs.getString("source_reference"),
                        payload(rs.getString("payload")), rs.getString("client_event_id"),
                        rs.getTimestamp("occurred_at").toInstant(), rs.getTimestamp("recorded_at").toInstant()))
                .stream().findFirst();
    }

    private java.util.Map<String, Object> payload(String value) {
        try {
            return mapper.readValue(value, java.util.Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("stored event payload is invalid", e);
        }
    }

    @Override
    public void save(LearningEvent event) {
        try {
            var params = new MapSqlParameterSource().addValue("id", event.id()).addValue("userId", event.userId())
                    .addValue("eventType", event.eventType().name()).addValue("skill", event.skill().name())
                    .addValue("sessionId", event.sessionId()).addValue("practiceSetId", event.practiceSetId())
                    .addValue("attemptId", event.attemptId()).addValue("questionId", event.questionId())
                    .addValue("roadmapItemId", event.roadmapItemId()).addValue("sourceReference", event.sourceReference())
                    .addValue("payload", mapper.writeValueAsString(event.payload())).addValue("clientEventId", event.clientEventId())
                    .addValue("occurredAt", Timestamp.from(event.occurredAt())).addValue("recordedAt", Timestamp.from(event.recordedAt()));
            jdbc.update("""
                    INSERT INTO learning_events(id,user_id,event_type,skill,session_id,practice_set_id,attempt_id,question_id,
                    roadmap_item_id,source_reference,payload,client_event_id,occurred_at,recorded_at)
                    VALUES(:id,:userId,:eventType,:skill,:sessionId,:practiceSetId,:attemptId,:questionId,:roadmapItemId,
                    :sourceReference,CAST(:payload AS jsonb),:clientEventId,:occurredAt,:recordedAt)
                    ON CONFLICT DO NOTHING
                    """, params);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("event payload cannot be serialized", e);
        }
    }
}
