package com.ieltsaitutor.results;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSubmissionReviewRepository implements SubmissionReviewRepository {
    private final JdbcTemplate jdbc;
    private static final RowMapper<SubmissionReview> MAPPER = (rs, row) -> new SubmissionReview(
            rs.getObject("id", UUID.class), rs.getObject("submission_id", UUID.class),
            rs.getObject("reviewer_user_id", UUID.class), rs.getInt("review_version"), nullable(rs, "overall_band"),
            nullable(rs, "fluency_coherence"), nullable(rs, "lexical_resource"), nullable(rs, "grammatical_range"),
            nullable(rs, "pronunciation"), rs.getString("reviewer_feedback"), rs.getString("criteria_json"),
            rs.getString("status"), instant(rs.getTimestamp("created_at")), instant(rs.getTimestamp("updated_at")));

    public JdbcSubmissionReviewRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static Double nullable(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private static java.time.Instant instant(Timestamp value) { return value == null ? java.time.Instant.now() : value.toInstant(); }

    @Override
    public SubmissionReview save(SubmissionReview review) {
        jdbc.update("""
                INSERT INTO submission_reviews(id, submission_id, reviewer_user_id, review_version,
                    overall_band, fluency_coherence, lexical_resource, grammatical_range, pronunciation,
                    reviewer_feedback, criteria_json, status, created_at, updated_at)
                VALUES(?,?,?,?,?,?,?,?,?,?,?::jsonb,?,?,?)
                """, review.id(), review.submissionId(), review.reviewerUserId(), review.reviewVersion(),
                review.overallBand(), review.fluencyCoherence(), review.lexicalResource(), review.grammaticalRange(),
                review.pronunciation(), review.reviewerFeedback(), review.criteriaJson() == null ? "{}" : review.criteriaJson(),
                review.status(), Timestamp.from(review.createdAt()), Timestamp.from(review.updatedAt()));
        return review;
    }

    @Override
    public Optional<SubmissionReview> findLatestBySubmission(UUID submissionId) {
        return query("WHERE submission_id = ? ORDER BY review_version DESC LIMIT 1", submissionId).stream().findFirst();
    }

    @Override
    public List<SubmissionReview> findBySubmission(UUID submissionId) {
        return query("WHERE submission_id = ? ORDER BY review_version ASC", submissionId);
    }

    @Override
    public int getNextReviewVersion(UUID submissionId) {
        Integer next = jdbc.queryForObject("SELECT COALESCE(MAX(review_version),0)+1 FROM submission_reviews WHERE submission_id = ?",
                Integer.class, submissionId);
        return next == null ? 1 : next;
    }

    private List<SubmissionReview> query(String suffix, UUID submissionId) {
        return jdbc.query("SELECT * FROM submission_reviews " + suffix, MAPPER, submissionId);
    }
}
