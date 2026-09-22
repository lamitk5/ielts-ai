package com.ieltsaitutor.writing;

import java.sql.Types;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcWritingRepository implements WritingRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();
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

    private Double number(JsonNode node, String field) {
        return node.path(field).isNumber() ? node.path(field).doubleValue() : null;
    }
}
