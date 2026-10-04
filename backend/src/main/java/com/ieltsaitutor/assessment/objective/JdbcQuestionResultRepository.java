package com.ieltsaitutor.assessment.objective;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcQuestionResultRepository implements QuestionResultRepository {
    private static final RowMapper<QuestionResult> MAPPER = (rs, rowNum) -> new QuestionResult(
            rs.getObject("id", UUID.class), rs.getObject("submission_id", UUID.class), rs.getObject("user_id", UUID.class),
            rs.getString("question_id"), rs.getString("question_type"), rs.getString("learner_answer"),
            rs.getString("normalized_learner_answer"), rs.getString("correct_answer"), rs.getBoolean("is_correct"),
            rs.getString("evidence_reference"), rs.getString("explanation"), rs.getString("scoring_policy_version"),
            rs.getTimestamp("created_at").toInstant());

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcQuestionResultRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional
    public List<QuestionResult> saveAll(List<QuestionResult> results) {
        if (results == null || results.isEmpty()) return List.of();
        for (QuestionResult result : results) {
            jdbc.update("""
                    INSERT INTO submission_question_results
                        (id,submission_id,user_id,question_id,question_type,learner_answer,
                         normalized_learner_answer,correct_answer,is_correct,evidence_reference,
                         explanation,scoring_policy_version,created_at)
                    VALUES (:id,:submissionId,:userId,:questionId,:questionType,:learnerAnswer,
                            :normalizedLearnerAnswer,:correctAnswer,:correct,:evidenceReference,
                            :explanation,:scoringPolicyVersion,:createdAt)
                    ON CONFLICT (submission_id, question_id) DO NOTHING
                    """, params(result));
        }
        return findBySubmission(results.getFirst().userId(), results.getFirst().submissionId());
    }

    @Override
    public List<QuestionResult> findByOwnedSubmission(UUID userId, UUID submissionId) {
        return findBySubmission(userId, submissionId);
    }

    private List<QuestionResult> findBySubmission(UUID userId, UUID submissionId) {
        return jdbc.query("""
                SELECT id,submission_id,user_id,question_id,question_type,learner_answer,
                    normalized_learner_answer,correct_answer,is_correct,evidence_reference,
                    explanation,scoring_policy_version,created_at
                FROM submission_question_results
                WHERE user_id = :userId AND submission_id = :submissionId
                ORDER BY question_id
                """, new MapSqlParameterSource().addValue("userId", userId).addValue("submissionId", submissionId), MAPPER);
    }

    private static MapSqlParameterSource params(QuestionResult result) {
        return new MapSqlParameterSource()
                .addValue("id", result.id()).addValue("submissionId", result.submissionId())
                .addValue("userId", result.userId()).addValue("questionId", result.questionId())
                .addValue("questionType", result.questionType()).addValue("learnerAnswer", result.learnerAnswer())
                .addValue("normalizedLearnerAnswer", result.normalizedLearnerAnswer())
                .addValue("correctAnswer", result.correctAnswer()).addValue("correct", result.correct())
                .addValue("evidenceReference", result.evidenceReference()).addValue("explanation", result.explanation())
                .addValue("scoringPolicyVersion", result.scoringPolicyVersion())
                .addValue("createdAt", Timestamp.from(result.createdAt()));
    }
}
