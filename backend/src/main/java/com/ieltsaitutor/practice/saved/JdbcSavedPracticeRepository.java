package com.ieltsaitutor.practice.saved;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSavedPracticeRepository implements SavedPracticeRepository {
    private static final RowMapper<SavedPractice> MAPPER = (rs, rowNum) -> new SavedPractice(
            rs.getObject("id", UUID.class),
            rs.getObject("user_id", UUID.class),
            rs.getString("published_set_id"),
            rs.getString("skill"),
            rs.getString("title"),
            rs.getTimestamp("saved_at").toInstant(),
            rs.getBoolean("available")
    );

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcSavedPracticeRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public SavedPractice save(SavedPractice item) {
        String sql = """
                INSERT INTO saved_practices (id, user_id, published_set_id, skill, title, saved_at)
                VALUES (:id, :userId, :publishedSetId, :skill, :title, :savedAt)
                ON CONFLICT (user_id, published_set_id) DO UPDATE SET
                    skill = EXCLUDED.skill,
                    title = EXCLUDED.title,
                    saved_at = EXCLUDED.saved_at
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", item.id())
                .addValue("userId", item.userId())
                .addValue("publishedSetId", item.publishedSetId())
                .addValue("skill", item.skill().toLowerCase(Locale.ROOT))
                .addValue("title", item.title())
                .addValue("savedAt", Timestamp.from(item.savedAt()));
        jdbc.update(sql, params);
        return item;
    }

    @Override
    public void delete(UUID userId, String publishedSetId) {
        String sql = "DELETE FROM saved_practices WHERE user_id = :userId AND published_set_id = :publishedSetId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("publishedSetId", publishedSetId);
        jdbc.update(sql, params);
    }

    @Override
    public List<SavedPractice> findByUser(UUID userId, String skillFilter, int page, int size) {
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.max(1, Math.min(size <= 0 ? 20 : size, 50));
        int offset = boundedPage * boundedSize;

        StringBuilder sql = new StringBuilder("""
                SELECT s.id, s.user_id, s.published_set_id, s.skill, s.title, s.saved_at,
                       COALESCE(p.active, FALSE) as available
                FROM saved_practices s
                LEFT JOIN practice_catalog_publications p ON s.published_set_id = p.published_set_id
                WHERE s.user_id = :userId
                """);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("limit", boundedSize)
                .addValue("offset", offset);

        if (skillFilter != null && !skillFilter.isBlank()) {
            sql.append(" AND LOWER(s.skill) = LOWER(:skill)");
            params.addValue("skill", skillFilter.trim());
        }

        sql.append(" ORDER BY s.saved_at DESC LIMIT :limit OFFSET :offset");
        return jdbc.query(sql.toString(), params, MAPPER);
    }

    @Override
    public Optional<SavedPractice> findByUserAndSetId(UUID userId, String publishedSetId) {
        String sql = """
                SELECT s.id, s.user_id, s.published_set_id, s.skill, s.title, s.saved_at,
                       COALESCE(p.active, FALSE) as available
                FROM saved_practices s
                LEFT JOIN practice_catalog_publications p ON s.published_set_id = p.published_set_id
                WHERE s.user_id = :userId AND s.published_set_id = :publishedSetId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("publishedSetId", publishedSetId);
        return jdbc.query(sql, params, MAPPER).stream().findFirst();
    }

    @Override
    public boolean isSaved(UUID userId, String publishedSetId) {
        String sql = "SELECT COUNT(*) FROM saved_practices WHERE user_id = :userId AND published_set_id = :publishedSetId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("publishedSetId", publishedSetId);
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public long countByUser(UUID userId) {
        String sql = "SELECT COUNT(*) FROM saved_practices WHERE user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }
}
