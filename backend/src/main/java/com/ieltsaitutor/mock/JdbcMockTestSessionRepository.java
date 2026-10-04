package com.ieltsaitutor.mock;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMockTestSessionRepository implements MockTestSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMockTestSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    private static final RowMapper<MockTestSession> ROW_MAPPER = (rs, rowNum) -> new MockTestSession(
            rs.getObject("id", UUID.class),
            rs.getObject("user_id", UUID.class),
            rs.getString("mock_test_id"),
            rs.getString("mock_test_version"),
            MockTestSessionStatus.valueOf(rs.getString("status")),
            rs.getInt("current_section_index"),
            rs.getInt("total_time_limit_seconds"),
            rs.getInt("elapsed_seconds"),
            toInstant(rs.getTimestamp("started_at")),
            toInstant(rs.getTimestamp("expires_at")),
            toInstant(rs.getTimestamp("completed_at")),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")),
            List.of()
    );

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }

    @Override
    public MockTestSession save(MockTestSession session) {
        String sql = """
                INSERT INTO mock_test_sessions (
                    id, user_id, mock_test_id, mock_test_version, status,
                    current_section_index, total_time_limit_seconds, elapsed_seconds,
                    started_at, expires_at, completed_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    current_section_index = EXCLUDED.current_section_index,
                    elapsed_seconds = EXCLUDED.elapsed_seconds,
                    started_at = EXCLUDED.started_at,
                    expires_at = EXCLUDED.expires_at,
                    completed_at = EXCLUDED.completed_at,
                    updated_at = EXCLUDED.updated_at
                """;
        jdbcTemplate.update(sql,
                session.id(),
                session.userId(),
                session.mockTestId(),
                session.mockTestVersion(),
                session.status().name(),
                session.currentSectionIndex(),
                session.totalTimeLimitSeconds(),
                session.elapsedSeconds(),
                session.startedAt() != null ? Timestamp.from(session.startedAt()) : null,
                session.expiresAt() != null ? Timestamp.from(session.expiresAt()) : null,
                session.completedAt() != null ? Timestamp.from(session.completedAt()) : null,
                Timestamp.from(session.createdAt()),
                Timestamp.from(session.updatedAt())
        );
        return session;
    }

    @Override
    public Optional<MockTestSession> findById(UUID id) {
        String sql = "SELECT * FROM mock_test_sessions WHERE id = ?";
        List<MockTestSession> list = jdbcTemplate.query(sql, ROW_MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<MockTestSession> findByUserAndId(UUID userId, UUID id) {
        String sql = "SELECT * FROM mock_test_sessions WHERE user_id = ? AND id = ?";
        List<MockTestSession> list = jdbcTemplate.query(sql, ROW_MAPPER, userId, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<MockTestSession> findActiveByUser(UUID userId) {
        String sql = "SELECT * FROM mock_test_sessions WHERE user_id = ? AND status IN ('NOT_STARTED', 'IN_PROGRESS', 'PAUSED') ORDER BY created_at DESC LIMIT 1";
        List<MockTestSession> list = jdbcTemplate.query(sql, ROW_MAPPER, userId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<MockTestSession> findByUser(UUID userId) {
        String sql = "SELECT * FROM mock_test_sessions WHERE user_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, userId);
    }

    @Override
    public void updateStatus(UUID id, MockTestSessionStatus status, Instant completedAt) {
        String sql = "UPDATE mock_test_sessions SET status = ?, completed_at = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), completedAt != null ? Timestamp.from(completedAt) : null, Timestamp.from(Instant.now()), id);
    }
}
