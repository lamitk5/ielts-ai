package com.ieltsaitutor.learning.draft;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLearningDraftRepository implements LearningDraftRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcLearningDraftRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<LearningDraft> findActive(UUID userId, String skill, String referenceId) {
        String sql = """
                SELECT * FROM learning_drafts
                WHERE user_id = :userId
                  AND skill = :skill
                  AND reference_id = :referenceId
                  AND status = 'ACTIVE'
                ORDER BY updated_at DESC
                LIMIT 1
                """;
        return jdbc.query(sql,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("skill", skill)
                        .addValue("referenceId", referenceId),
                this::mapRow
        ).stream().findFirst();
    }

    @Override
    public Optional<LearningDraft> findById(UUID id) {
        String sql = "SELECT * FROM learning_drafts WHERE id = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", id), this::mapRow)
                .stream().findFirst();
    }

    @Override
    public LearningDraft save(LearningDraft draft) {
        String sql = """
                INSERT INTO learning_drafts (
                    id, user_id, skill, reference_id, content_snapshot,
                    version, status, created_at, updated_at, expires_at
                ) VALUES (
                    :id, :userId, :skill, :referenceId, :contentSnapshot,
                    :version, :status, :createdAt, :updatedAt, :expiresAt
                ) ON CONFLICT (id) DO UPDATE SET
                    content_snapshot = EXCLUDED.content_snapshot,
                    version = EXCLUDED.version,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at,
                    expires_at = EXCLUDED.expires_at
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", draft.id())
                .addValue("userId", draft.userId())
                .addValue("skill", draft.skill())
                .addValue("referenceId", draft.referenceId())
                .addValue("contentSnapshot", draft.contentSnapshot())
                .addValue("version", draft.version())
                .addValue("status", draft.status().name())
                .addValue("createdAt", Timestamp.from(draft.createdAt()))
                .addValue("updatedAt", Timestamp.from(draft.updatedAt()))
                .addValue("expiresAt", draft.expiresAt() != null ? Timestamp.from(draft.expiresAt()) : null);

        jdbc.update(sql, params);
        return draft;
    }

    @Override
    public void delete(UUID id) {
        String sql = "UPDATE learning_drafts SET status = 'DELETED', updated_at = CURRENT_TIMESTAMP WHERE id = :id";
        jdbc.update(sql, new MapSqlParameterSource("id", id));
    }

    private LearningDraft mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new LearningDraft(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("skill"),
                rs.getString("reference_id"),
                rs.getString("content_snapshot"),
                rs.getLong("version"),
                LearningDraft.DraftStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getTimestamp("expires_at") != null ? rs.getTimestamp("expires_at").toInstant() : null
        );
    }
}
