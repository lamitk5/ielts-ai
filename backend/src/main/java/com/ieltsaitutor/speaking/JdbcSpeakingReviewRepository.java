package com.ieltsaitutor.speaking;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSpeakingReviewRepository implements SpeakingReviewRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSpeakingReviewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    private static final RowMapper<SpeakingReview> ROW_MAPPER = (rs, rowNum) -> new SpeakingReview(
            rs.getObject("id", UUID.class),
            rs.getObject("submission_id", UUID.class),
            rs.getObject("reviewer_user_id", UUID.class),
            rs.getInt("review_version"),
            getNullableDouble(rs, "overall_band"),
            getNullableDouble(rs, "fluency_coherence"),
            getNullableDouble(rs, "lexical_resource"),
            getNullableDouble(rs, "grammatical_range"),
            getNullableDouble(rs, "pronunciation"),
            rs.getString("reviewer_feedback"),
            rs.getString("criteria_json"),
            rs.getString("status"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at"))
    );

    private static Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        double val = rs.getDouble(column);
        return rs.wasNull() ? null : val;
    }

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : Instant.now();
    }

    @Override
    public SpeakingReview save(SpeakingReview review) {
        String sql = """
                INSERT INTO submission_reviews (
                    id, submission_id, reviewer_user_id, review_version,
                    overall_band, fluency_coherence, lexical_resource,
                    grammatical_range, pronunciation, reviewer_feedback,
                    criteria_json, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                """;
        jdbcTemplate.update(sql,
                review.id(),
                review.submissionId(),
                review.reviewerUserId(),
                review.reviewVersion(),
                review.overallBand(),
                review.fluencyCoherence(),
                review.lexicalResource(),
                review.grammaticalRange(),
                review.pronunciation(),
                review.reviewerFeedback(),
                review.criteriaJson(),
                review.status(),
                Timestamp.from(review.createdAt()),
                Timestamp.from(review.updatedAt())
        );
        return review;
    }

    @Override
    public Optional<SpeakingReview> findLatestBySubmission(UUID submissionId) {
        String sql = "SELECT * FROM submission_reviews WHERE submission_id = ? ORDER BY review_version DESC LIMIT 1";
        List<SpeakingReview> list = jdbcTemplate.query(sql, ROW_MAPPER, submissionId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<SpeakingReview> findBySubmission(UUID submissionId) {
        String sql = "SELECT * FROM submission_reviews WHERE submission_id = ? ORDER BY review_version ASC";
        return jdbcTemplate.query(sql, ROW_MAPPER, submissionId);
    }

    @Override
    public int getNextReviewVersion(UUID submissionId) {
        String sql = "SELECT COALESCE(MAX(review_version), 0) + 1 FROM submission_reviews WHERE submission_id = ?";
        Integer next = jdbcTemplate.queryForObject(sql, Integer.class, submissionId);
        return next != null ? next : 1;
    }
}
