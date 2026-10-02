package com.ieltsaitutor.writing;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JdbcWritingEvaluationRepository implements WritingEvaluationRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RowMapper<WritingEvaluationResult> rowMapper;

    public JdbcWritingEvaluationRepository(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, new ObjectMapper());
    }

    public JdbcWritingEvaluationRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.rowMapper = (rs, rowNum) -> {
            try {
                UUID id = (UUID) rs.getObject("id");
                UUID versionId = (UUID) rs.getObject("version_id");
                int evalVersion = rs.getInt("evaluation_version");
                Double band = rs.getObject("overall_band_estimate") != null ? rs.getDouble("overall_band_estimate") : null;

                String criteriaJson = rs.getString("criteria");
                Map<String, String> criteria = criteriaJson != null
                        ? this.objectMapper.readValue(criteriaJson, new TypeReference<Map<String, String>>() {})
                        : Map.of();

                List<String> strengths = readList(rs.getString("strengths"));
                List<String> issues = readList(rs.getString("issues"));
                List<String> suggestions = readList(rs.getString("suggestions"));
                List<String> evidenceSpans = readList(rs.getString("evidence_spans"));
                List<String> priorityImprovements = readList(rs.getString("priority_improvements"));

                String grounding = rs.getString("grounding_status");
                String disclaimer = rs.getString("disclaimer");
                String status = rs.getString("status");
                Timestamp createdAt = rs.getTimestamp("created_at");

                return new WritingEvaluationResult(
                        id,
                        versionId,
                        evalVersion,
                        band,
                        criteria,
                        strengths,
                        issues,
                        suggestions,
                        evidenceSpans,
                        priorityImprovements,
                        grounding,
                        disclaimer,
                        status,
                        createdAt != null ? createdAt.toInstant() : java.time.Instant.now());
            } catch (Exception e) {
                throw new SQLException("Failed to map WritingEvaluationResult", e);
            }
        };
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Override
    public WritingEvaluationResult save(WritingEvaluationResult evaluation) {
        String sql = """
                INSERT INTO writing_evaluations
                (id, version_id, evaluation_version, overall_band_estimate, criteria, strengths, issues, suggestions,
                 evidence_spans, priority_improvements, grounding_status, disclaimer, status, created_at)
                VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql,
                evaluation.id(),
                evaluation.versionId(),
                evaluation.evaluationVersion(),
                evaluation.overallBandEstimate(),
                toJson(evaluation.criteria()),
                toJson(evaluation.strengths()),
                toJson(evaluation.issues()),
                toJson(evaluation.suggestions()),
                toJson(evaluation.evidenceSpans()),
                toJson(evaluation.priorityImprovements()),
                evaluation.groundingStatus(),
                evaluation.disclaimer(),
                evaluation.status(),
                Timestamp.from(evaluation.createdAt()));
        return evaluation;
    }

    @Override
    public Optional<WritingEvaluationResult> findById(UUID id) {
        String sql = "SELECT * FROM writing_evaluations WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<WritingEvaluationResult> findByVersionId(UUID versionId) {
        String sql = "SELECT * FROM writing_evaluations WHERE version_id = ? ORDER BY evaluation_version DESC";
        return jdbcTemplate.query(sql, rowMapper, versionId);
    }

    @Override
    public Optional<WritingEvaluationResult> findLatestByVersionId(UUID versionId) {
        String sql = "SELECT * FROM writing_evaluations WHERE version_id = ? ORDER BY evaluation_version DESC LIMIT 1";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, versionId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public int getNextEvaluationVersion(UUID versionId) {
        String sql = "SELECT COALESCE(MAX(evaluation_version), 0) + 1 FROM writing_evaluations WHERE version_id = ?";
        Integer next = jdbcTemplate.queryForObject(sql, Integer.class, versionId);
        return next != null ? next : 1;
    }
}
