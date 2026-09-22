package com.ieltsaitutor.writing;

import java.sql.Types;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcWritingRepository implements WritingRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcWritingRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void save(WritingAssessment assessment) {
        String payload = "{\"overallBandEstimate\":" + (assessment.overallBandEstimate() == null ? "null" : assessment.overallBandEstimate()) + "}";
        jdbc.update("""
                INSERT INTO writing_submissions(id,user_id,task_id,response_text,word_count,assessment_status,assessment_payload,created_at)
                VALUES(:id,:userId,:taskId,:responseText,:wordCount,:status,CAST(:payload AS jsonb),:createdAt)
                """, new MapSqlParameterSource().addValue("id", UUID.randomUUID()).addValue("userId", assessment.userId())
                .addValue("taskId", assessment.taskId()).addValue("responseText", assessment.submittedText() == null ? "" : assessment.submittedText())
                .addValue("wordCount", assessment.wordCount()).addValue("status", assessment.status()).addValue("payload", payload)
                .addValue("createdAt", assessment.createdAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE));
    }

    @Override
    public List<WritingAssessment> findByUser(UUID userId) { return List.of(); }
}
