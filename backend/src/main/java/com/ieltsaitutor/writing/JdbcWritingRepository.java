package com.ieltsaitutor.writing;

import java.sql.Types;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcWritingRepository implements WritingRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Autowired
    public JdbcWritingRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(WritingAssessment assessment) {
        String payload;
        try { payload = objectMapper.writeValueAsString(assessment); }
        catch (Exception exception) { throw new IllegalStateException("Không thể lưu kết quả Writing.", exception); }
        jdbc.update("""
                INSERT INTO writing_submissions(id,user_id,task_id,response_text,word_count,assessment_status,assessment_payload,created_at)
                VALUES(:id,:userId,:taskId,:responseText,:wordCount,:status,CAST(:payload AS jsonb),:createdAt)
                """, new MapSqlParameterSource().addValue("id", UUID.randomUUID()).addValue("userId", assessment.userId())
                .addValue("taskId", assessment.taskId()).addValue("responseText", assessment.submittedText() == null ? "" : assessment.submittedText())
                .addValue("wordCount", assessment.wordCount()).addValue("status", assessment.status()).addValue("payload", payload)
                .addValue("createdAt", assessment.createdAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE));
    }

    @Override
    public List<WritingAssessment> findByUser(UUID userId) {
        return jdbc.query("""
                SELECT user_id, task_id, response_text, word_count, assessment_payload, created_at
                FROM writing_submissions WHERE user_id = :userId ORDER BY created_at DESC
                """, new MapSqlParameterSource("userId", userId), (rs, rowNum) -> map(rs.getObject("user_id", UUID.class),
                        rs.getString("task_id"), rs.getString("response_text"), rs.getInt("word_count"),
                        rs.getString("assessment_payload"), rs.getTimestamp("created_at").toInstant()));
    }

    @Override
    @Transactional
    public WritingAttempt start(UUID userId, String taskId) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(:lockKey, CAST(0 AS bigint)))",
                new MapSqlParameterSource("lockKey", userId + ":" + taskId),
                (rs, rowNum) -> null);
        Optional<WritingAttempt> active = jdbc.query("""
                SELECT id,user_id,task_id,response_text,word_count,assessment_status,assessment_payload,attempt_status,created_at,submitted_at
                FROM writing_submissions WHERE id = (SELECT id FROM writing_submissions
                WHERE user_id = :userId AND task_id = :taskId AND attempt_status = 'IN_PROGRESS'
                ORDER BY created_at DESC LIMIT 1)
                """, new MapSqlParameterSource().addValue("userId", userId).addValue("taskId", taskId),
                (rs, row) -> mapAttempt(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class), rs.getString("task_id"),
                        rs.getString("response_text"), rs.getInt("word_count"), rs.getString("assessment_status"),
                        rs.getString("assessment_payload"), rs.getString("attempt_status"), rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("submitted_at") == null ? null : rs.getTimestamp("submitted_at").toInstant())).stream().findFirst();
        if (active.isPresent()) return active.get();
        UUID id = UUID.randomUUID();
        java.time.Instant created = java.time.Instant.now();
        jdbc.update("""
                INSERT INTO writing_submissions(id,user_id,task_id,response_text,word_count,assessment_status,assessment_payload,created_at,attempt_status)
                VALUES(:id,:userId,:taskId,'',0,'UNAVAILABLE','{}'::jsonb,:createdAt,'IN_PROGRESS')
                """, new MapSqlParameterSource().addValue("id", id).addValue("userId", userId).addValue("taskId", taskId)
                .addValue("createdAt", created.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE));
        return new WritingAttempt(id, userId, taskId, taskType(taskId), "IN_PROGRESS", "", 0, created, null, null);
    }

    @Override
    public Optional<WritingAttempt> find(UUID attemptId, UUID userId) {
        return jdbc.query("""
                SELECT id,user_id,task_id,response_text,word_count,assessment_status,assessment_payload,attempt_status,created_at,submitted_at
                FROM writing_submissions WHERE id = :id AND user_id = :userId
                """, new MapSqlParameterSource().addValue("id", attemptId).addValue("userId", userId),
                (rs, row) -> mapAttempt(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class), rs.getString("task_id"),
                        rs.getString("response_text"), rs.getInt("word_count"), rs.getString("assessment_status"),
                        rs.getString("assessment_payload"), rs.getString("attempt_status"), rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("submitted_at") == null ? null : rs.getTimestamp("submitted_at").toInstant())).stream().findFirst();
    }

    @Override
    public WritingAttempt saveDraft(WritingAttempt attempt) {
        jdbc.update("UPDATE writing_submissions SET response_text=:responseText,word_count=:wordCount WHERE id=:id AND attempt_status='IN_PROGRESS'",
                new MapSqlParameterSource().addValue("id", attempt.id()).addValue("responseText", attempt.responseText()).addValue("wordCount", attempt.wordCount()));
        return attempt;
    }

    @Override
    public WritingAttempt complete(WritingAttempt attempt, WritingAssessment assessment) {
        String payload;
        try { payload = objectMapper.writeValueAsString(assessment); }
        catch (Exception exception) { throw new IllegalStateException("Không thể lưu kết quả Writing.", exception); }
        java.time.Instant submitted = java.time.Instant.now();
        jdbc.update("""
                UPDATE writing_submissions SET response_text=:responseText,word_count=:wordCount,assessment_status=:status,
                    assessment_payload=CAST(:payload AS jsonb),attempt_status='FEEDBACK_READY',submitted_at=:submittedAt
                WHERE id=:id AND attempt_status='IN_PROGRESS'
                """, new MapSqlParameterSource().addValue("id", attempt.id()).addValue("responseText", assessment.submittedText())
                .addValue("wordCount", assessment.wordCount()).addValue("status", assessment.status()).addValue("payload", payload)
                .addValue("submittedAt", submitted.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE));
        return new WritingAttempt(attempt.id(), attempt.userId(), attempt.taskId(), attempt.taskType(), "FEEDBACK_READY",
                assessment.submittedText(), assessment.wordCount(), attempt.createdAt(), submitted, assessment);
    }

    private WritingAssessment map(UUID userId, String taskId, String responseText, int wordCount,
            String payload, java.time.Instant createdAt) {
        try {
            JsonNode node = objectMapper.readTree(payload == null ? "{}" : payload);
            return new WritingAssessment(text(node, "status", "UNAVAILABLE"), userId, taskId,
                    number(node, "overallBandEstimate"), objectMapper.convertValue(node.path("criteria"), java.util.Map.class),
                    objectMapper.convertValue(node.path("strengths"), java.util.List.class),
                    objectMapper.convertValue(node.path("issues"), java.util.List.class),
                    objectMapper.convertValue(node.path("suggestions"), java.util.List.class),
                    objectMapper.convertValue(node.path("citations"), java.util.List.class),
                    text(node, "groundingStatus", "NOT_ENABLED"), node.path("grounded").asBoolean(false),
                    text(node, "disclaimer", "Band ước lượng — không phải điểm thi chính thức."), createdAt, responseText, wordCount);
        } catch (Exception exception) {
            return new WritingAssessment("UNAVAILABLE", userId, taskId, null, java.util.Map.of(), java.util.List.of(),
                    java.util.List.of(), java.util.List.of(), java.util.List.of(), "NOT_ENABLED", false,
                    "Không thể đọc kết quả đánh giá đã lưu.", createdAt, responseText, wordCount);
        }
    }

    private String text(JsonNode node, String field, String fallback) {
        return node.path(field).isTextual() ? node.path(field).asText() : fallback;
    }

    private WritingAttempt mapAttempt(UUID id, UUID userId, String taskId, String responseText, int wordCount,
            String assessmentStatus, String payload, String attemptStatus, java.time.Instant createdAt, java.time.Instant submittedAt) {
        WritingAssessment assessment = "IN_PROGRESS".equals(attemptStatus) ? null
                : mapAssessment(userId, taskId, responseText, wordCount, assessmentStatus, payload, createdAt);
        return new WritingAttempt(id, userId, taskId, taskType(taskId), attemptStatus, responseText, wordCount, createdAt, submittedAt, assessment);
    }

    private WritingAssessment mapAssessment(UUID userId, String taskId, String responseText, int wordCount,
            String assessmentStatus, String payload, java.time.Instant createdAt) {
        JsonNode node = read(payload);
        return new WritingAssessment(text(node, "status", assessmentStatus), userId, taskId,
                number(node, "overallBandEstimate"), objectMapper.convertValue(node.path("criteria"), java.util.Map.class),
                objectMapper.convertValue(node.path("strengths"), java.util.List.class),
                objectMapper.convertValue(node.path("issues"), java.util.List.class),
                objectMapper.convertValue(node.path("suggestions"), java.util.List.class),
                objectMapper.convertValue(node.path("citations"), java.util.List.class),
                text(node, "groundingStatus", "NOT_ENABLED"), node.path("grounded").asBoolean(false),
                text(node, "disclaimer", "Band ước lượng — không phải điểm thi chính thức."), createdAt, responseText, wordCount);
    }

    private JsonNode read(String payload) { try { return objectMapper.readTree(payload == null ? "{}" : payload); } catch (Exception ignored) { return objectMapper.createObjectNode(); } }

    private String taskType(String taskId) { return taskId != null && taskId.startsWith("task-1") ? "TASK_1" : "TASK_2"; }

    private Double number(JsonNode node, String field) {
        return node.path(field).isNumber() ? node.path(field).doubleValue() : null;
    }
}
