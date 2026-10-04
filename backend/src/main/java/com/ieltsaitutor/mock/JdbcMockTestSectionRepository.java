package com.ieltsaitutor.mock;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMockTestSectionRepository implements MockTestSectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMockTestSectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    private static final RowMapper<MockTestSection> ROW_MAPPER = (rs, rowNum) -> new MockTestSection(
            rs.getObject("id", UUID.class),
            rs.getObject("session_id", UUID.class),
            rs.getInt("section_order"),
            rs.getString("skill"),
            rs.getString("practice_id"),
            rs.getString("practice_version_id"),
            rs.getString("published_set_id"),
            rs.getObject("submission_id", UUID.class),
            rs.getInt("time_limit_seconds"),
            MockTestSectionStatus.valueOf(rs.getString("status")),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at"))
    );

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }

    @Override
    public MockTestSection save(MockTestSection section) {
        String sql = """
                INSERT INTO mock_test_sections (
                    id, session_id, section_order, skill, practice_id,
                    practice_version_id, published_set_id, submission_id,
                    time_limit_seconds, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (session_id, section_order) DO UPDATE SET
                    submission_id = EXCLUDED.submission_id,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """;
        jdbcTemplate.update(sql,
                section.id(),
                section.sessionId(),
                section.sectionOrder(),
                section.skill(),
                section.practiceId(),
                section.practiceVersionId(),
                section.publishedSetId(),
                section.submissionId(),
                section.timeLimitSeconds(),
                section.status().name(),
                Timestamp.from(section.createdAt()),
                Timestamp.from(section.updatedAt())
        );
        return section;
    }

    @Override
    public List<MockTestSection> saveAll(List<MockTestSection> sections) {
        List<MockTestSection> saved = new ArrayList<>();
        for (MockTestSection sec : sections) {
            saved.add(save(sec));
        }
        return saved;
    }

    @Override
    public List<MockTestSection> findBySessionId(UUID sessionId) {
        String sql = "SELECT * FROM mock_test_sections WHERE session_id = ? ORDER BY section_order ASC";
        return jdbcTemplate.query(sql, ROW_MAPPER, sessionId);
    }

    @Override
    public Optional<MockTestSection> findBySessionIdAndOrder(UUID sessionId, int sectionOrder) {
        String sql = "SELECT * FROM mock_test_sections WHERE session_id = ? AND section_order = ?";
        List<MockTestSection> list = jdbcTemplate.query(sql, ROW_MAPPER, sessionId, sectionOrder);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<MockTestSection> findBySubmissionId(UUID submissionId) {
        String sql = "SELECT * FROM mock_test_sections WHERE submission_id = ?";
        List<MockTestSection> list = jdbcTemplate.query(sql, ROW_MAPPER, submissionId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
