package com.ieltsaitutor.learning.vocabulary;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcVocabularyRepository implements VocabularyRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcVocabularyRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<VocabularyItem> findByUser(UUID userId, String query, VocabularyStatus status, String sort) {
        String order = "created_at DESC";
        if ("word".equalsIgnoreCase(sort)) order = "normalized_word ASC";
        if ("reviewed".equalsIgnoreCase(sort)) order = "last_reviewed_at DESC NULLS FIRST";
        String sql = "SELECT * FROM vocabulary_items WHERE user_id=:userId "
                + "AND (:query IS NULL OR normalized_word LIKE :pattern OR word ILIKE :pattern) "
                + "AND (:status IS NULL OR status=:status) ORDER BY " + order;
        MapSqlParameterSource p = params(userId).addValue("query", blankToNull(query))
                .addValue("pattern", query == null || query.isBlank() ? null : "%" + query.trim().toLowerCase(java.util.Locale.ROOT) + "%")
                .addValue("status", status == null ? null : status.name());
        return jdbc.query(sql, p, (rs, row) -> map(rs));
    }

    @Override
    public Optional<VocabularyItem> findByIdAndUser(UUID userId, UUID id) {
        return jdbc.query("SELECT * FROM vocabulary_items WHERE user_id=:userId AND id=:id",
                params(userId).addValue("id", id), (rs, row) -> map(rs)).stream().findFirst();
    }

    @Override
    public VocabularyItem save(VocabularyItem item) {
        String sql = """
                INSERT INTO vocabulary_items (id,user_id,word,normalized_word,meaning,example_sentence,note,source,source_reference_id,status,created_at,updated_at,last_reviewed_at,review_count)
                VALUES (:id,:userId,:word,:normalizedWord,:meaning,:exampleSentence,:note,:source,:sourceReferenceId,:status,:createdAt,:updatedAt,:lastReviewedAt,:reviewCount)
                ON CONFLICT (id) DO UPDATE SET word=EXCLUDED.word, normalized_word=EXCLUDED.normalized_word,
                meaning=EXCLUDED.meaning, example_sentence=EXCLUDED.example_sentence, note=EXCLUDED.note,
                source=EXCLUDED.source, source_reference_id=EXCLUDED.source_reference_id, status=EXCLUDED.status,
                updated_at=EXCLUDED.updated_at, last_reviewed_at=EXCLUDED.last_reviewed_at, review_count=EXCLUDED.review_count
                """;
        jdbc.update(sql, params(item.userId()).addValue("id", item.id()).addValue("word", item.word())
                .addValue("normalizedWord", item.normalizedWord()).addValue("meaning", item.meaning())
                .addValue("exampleSentence", item.exampleSentence()).addValue("note", item.note())
                .addValue("source", item.source()).addValue("sourceReferenceId", item.sourceReferenceId())
                .addValue("status", item.status().name()).addValue("createdAt", Timestamp.from(item.createdAt()))
                .addValue("updatedAt", Timestamp.from(item.updatedAt())).addValue("lastReviewedAt", item.lastReviewedAt() == null ? null : Timestamp.from(item.lastReviewedAt()))
                .addValue("reviewCount", item.reviewCount()));
        return item;
    }

    @Override public void delete(UUID userId, UUID id) {
        jdbc.update("DELETE FROM vocabulary_items WHERE user_id=:userId AND id=:id", params(userId).addValue("id", id));
    }

    private MapSqlParameterSource params(UUID userId) { return new MapSqlParameterSource("userId", userId); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private VocabularyItem map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new VocabularyItem((UUID) rs.getObject("id"), (UUID) rs.getObject("user_id"), rs.getString("word"),
                rs.getString("normalized_word"), rs.getString("meaning"), rs.getString("example_sentence"), rs.getString("note"),
                rs.getString("source"), rs.getString("source_reference_id"), VocabularyStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(),
                rs.getTimestamp("last_reviewed_at") == null ? null : rs.getTimestamp("last_reviewed_at").toInstant(), rs.getInt("review_count"));
    }
}
