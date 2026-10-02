package com.ieltsaitutor.submission;

import java.sql.Timestamp;
import java.time.Instant;
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
public class JdbcSubmissionDraftRepository implements SubmissionDraftRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RowMapper<SubmissionDraftSnapshot> rowMapper = (rs, row) -> new SubmissionDraftSnapshot(
            rs.getObject("submission_id", UUID.class), rs.getObject("user_id", UUID.class),
            parsePayload(rs.getString("draft_payload")), rs.getLong("revision"),
            rs.getString("idempotency_key"), rs.getTimestamp("updated_at").toInstant());

    public JdbcSubmissionDraftRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SubmissionDraftSnapshot> findBySubmissionId(UUID submissionId) {
        return jdbc.query("SELECT submission_id,user_id,draft_payload,revision,idempotency_key,updated_at "
                        + "FROM practice_submission_drafts WHERE submission_id = :id",
                new MapSqlParameterSource("id", submissionId), rowMapper).stream().findFirst();
    }

    @Override
    public Optional<SubmissionDraftSnapshot> saveIfRevision(UUID submissionId, UUID userId, Map<String, String> payload,
            long expectedRevision, String idempotencyKey, Instant updatedAt) {
        String sql = """
                INSERT INTO practice_submission_drafts
                    (submission_id,user_id,draft_payload,revision,idempotency_key,updated_at)
                VALUES(:submissionId,:userId,CAST(:payload AS jsonb),1,:idempotencyKey,:updatedAt)
                ON CONFLICT (submission_id) DO UPDATE SET
                    draft_payload = EXCLUDED.draft_payload,
                    revision = practice_submission_drafts.revision + 1,
                    idempotency_key = EXCLUDED.idempotency_key,
                    updated_at = EXCLUDED.updated_at
                WHERE practice_submission_drafts.user_id = :userId
                  AND practice_submission_drafts.revision = :expectedRevision
                RETURNING submission_id,user_id,draft_payload,revision,idempotency_key,updated_at
                """;
        List<SubmissionDraftSnapshot> saved = jdbc.query(sql, new MapSqlParameterSource()
                .addValue("submissionId", submissionId).addValue("userId", userId)
                .addValue("payload", json(payload)).addValue("expectedRevision", expectedRevision)
                .addValue("idempotencyKey", idempotencyKey).addValue("updatedAt", Timestamp.from(updatedAt)), rowMapper);
        return saved.stream().findFirst();
    }

    private String json(Map<String, String> payload) {
        try { return mapper.writeValueAsString(payload == null ? Map.of() : payload); }
        catch (Exception error) { throw new IllegalArgumentException("Invalid submission draft", error); }
    }

    private Map<String, String> parsePayload(String payload) {
        try { return mapper.readValue(payload == null ? "{}" : payload, new TypeReference<>() {}); }
        catch (Exception error) { return Map.of(); }
    }
}
