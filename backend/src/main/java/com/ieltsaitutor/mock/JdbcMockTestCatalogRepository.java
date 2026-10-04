package com.ieltsaitutor.mock;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMockTestCatalogRepository implements MockTestCatalogRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcMockTestCatalogRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public List<MockTestCatalogItem> findAll(boolean publishedOnly) {
        String sql = "SELECT * FROM mock_test_definitions WHERE (:publishedOnly = false OR published = true) ORDER BY created_at DESC";
        return jdbc.query(sql, new MapSqlParameterSource("publishedOnly", publishedOnly), (rs, row) -> baseMap(rs)).stream().map(this::withSections).toList();
    }
    @Override public Optional<MockTestCatalogItem> findBySlug(String slug) { return queryOne("slug", slug); }
    @Override public Optional<MockTestCatalogItem> findById(UUID id) { return jdbc.query("SELECT * FROM mock_test_definitions WHERE id=:id", new MapSqlParameterSource("id", id), (rs, row) -> baseMap(rs)).stream().map(this::withSections).findFirst(); }
    private Optional<MockTestCatalogItem> queryOne(String column, String value) { return jdbc.query("SELECT * FROM mock_test_definitions WHERE " + column + "=:value", new MapSqlParameterSource("value", value), (rs, row) -> baseMap(rs)).stream().map(this::withSections).findFirst(); }
    @Override public MockTestCatalogItem save(MockTestCatalogItem item) {
        jdbc.update("""
                INSERT INTO mock_test_definitions (id,slug,title,version,total_time_limit_seconds,published,created_at,updated_at)
                VALUES (:id,:slug,:title,:version,:total,:published,:createdAt,:updatedAt)
                ON CONFLICT (id) DO UPDATE SET slug=EXCLUDED.slug,title=EXCLUDED.title,version=EXCLUDED.version,
                total_time_limit_seconds=EXCLUDED.total_time_limit_seconds,published=EXCLUDED.published,updated_at=EXCLUDED.updated_at
                """, params(item).addValue("id", item.id()).addValue("slug", item.slug()).addValue("title", item.title())
                .addValue("version", item.version()).addValue("total", item.totalTimeLimitSeconds()).addValue("published", item.published())
                .addValue("createdAt", Timestamp.from(item.createdAt())).addValue("updatedAt", Timestamp.from(item.updatedAt())));
        jdbc.update("DELETE FROM mock_test_definition_sections WHERE mock_test_id=:id", new MapSqlParameterSource("id", item.id()));
        for (MockTestCatalogItem.Section section : item.sections()) jdbc.update("INSERT INTO mock_test_definition_sections (id,mock_test_id,section_order,skill,practice_set_id,time_limit_seconds) VALUES (:sectionId,:id,:order,:skill,:practice,:time)",
                new MapSqlParameterSource("sectionId", UUID.randomUUID()).addValue("id", item.id()).addValue("order", section.order()).addValue("skill", section.skill()).addValue("practice", section.practiceSetId()).addValue("time", section.timeLimitSeconds()));
        return item;
    }
    @Override public void delete(UUID id) { jdbc.update("DELETE FROM mock_test_definitions WHERE id=:id", new MapSqlParameterSource("id", id)); }
    private MapSqlParameterSource params(MockTestCatalogItem item) { return new MapSqlParameterSource(); }
    private MockTestCatalogItem baseMap(java.sql.ResultSet rs) throws java.sql.SQLException {
        UUID id = (UUID) rs.getObject("id");
        return new MockTestCatalogItem(id, rs.getString("slug"), rs.getString("title"), rs.getString("version"), rs.getInt("total_time_limit_seconds"), rs.getBoolean("published"), rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(), List.of());
    }
    private MockTestCatalogItem withSections(MockTestCatalogItem item) {
        List<MockTestCatalogItem.Section> sections = jdbc.query("SELECT * FROM mock_test_definition_sections WHERE mock_test_id=:id ORDER BY section_order", new MapSqlParameterSource("id", item.id()), (section, row) -> new MockTestCatalogItem.Section(section.getInt("section_order"), section.getString("skill"), section.getString("practice_set_id"), section.getInt("time_limit_seconds")));
        return new MockTestCatalogItem(item.id(), item.slug(), item.title(), item.version(), item.totalTimeLimitSeconds(), item.published(), item.createdAt(), item.updatedAt(), sections);
    }
}
