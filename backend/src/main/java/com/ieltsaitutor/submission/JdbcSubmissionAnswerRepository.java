package com.ieltsaitutor.submission;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JdbcSubmissionAnswerRepository implements SubmissionAnswerRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RowMapper<SubmissionAnswerSnapshot> rowMapper = (rs, row) -> new SubmissionAnswerSnapshot(
            rs.getObject("submission_id", UUID.class), rs.getObject("user_id", UUID.class),
            parseAnswers(rs.getString("answer_payload")), rs.getString("content_hash"),
            rs.getTimestamp("submitted_at").toInstant());

    public JdbcSubmissionAnswerRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public SubmissionAnswerSnapshot save(SubmissionAnswerSnapshot snapshot) {
        jdbc.update("""
                INSERT INTO submission_answers(submission_id,user_id,answer_payload,content_hash,submitted_at)
                VALUES(:submissionId,:userId,CAST(:answers AS jsonb),:contentHash,:submittedAt)
                ON CONFLICT (submission_id) DO NOTHING
                """, new MapSqlParameterSource().addValue("submissionId", snapshot.submissionId())
                .addValue("userId", snapshot.userId()).addValue("answers", json(snapshot.answers()))
                .addValue("contentHash", snapshot.contentHash())
                .addValue("submittedAt", Timestamp.from(snapshot.submittedAt())));
        return findBySubmissionId(snapshot.submissionId()).orElseThrow(() -> new IllegalArgumentException("Answer snapshot not found"));
    }

    @Override
    public Optional<SubmissionAnswerSnapshot> findBySubmissionId(UUID submissionId) {
        List<SubmissionAnswerSnapshot> rows = jdbc.query("SELECT submission_id,user_id,answer_payload,content_hash,submitted_at "
                        + "FROM submission_answers WHERE submission_id = :id",
                new MapSqlParameterSource("id", submissionId), rowMapper);
        return rows.stream().findFirst();
    }

    private String json(Map<String, String> answers) {
        try { return mapper.writeValueAsString(answers == null ? Map.of() : answers); }
        catch (Exception error) { throw new IllegalArgumentException("Invalid answer snapshot", error); }
    }

    private Map<String, String> parseAnswers(String answers) {
        try { return mapper.readValue(answers == null ? "{}" : answers, new TypeReference<>() {}); }
        catch (Exception error) { return Map.of(); }
    }
}
