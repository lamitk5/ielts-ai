package com.ieltsaitutor.writing;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcWritingVersionRepository implements WritingVersionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcWritingVersionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<WritingSubmissionVersion> ROW_MAPPER = (rs, rowNum) -> new WritingSubmissionVersion(
            (UUID) rs.getObject("id"),
            (UUID) rs.getObject("submission_id"),
            rs.getInt("version_number"),
            (UUID) rs.getObject("parent_version_id"),
            rs.getString("response_text"),
            rs.getInt("word_count"),
            rs.getString("content_hash"),
            rs.getTimestamp("created_at").toInstant());

    @Override
    public WritingSubmissionVersion save(WritingSubmissionVersion version) {
        String sql = """
                INSERT INTO writing_submission_versions
                (id, submission_id, version_number, parent_version_id, response_text, word_count, content_hash, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql,
                version.id(),
                version.submissionId(),
                version.versionNumber(),
                version.parentVersionId(),
                version.responseText(),
                version.wordCount(),
                version.contentHash(),
                Timestamp.from(version.createdAt()));
        return version;
    }

    @Override
    public Optional<WritingSubmissionVersion> findById(UUID id) {
        String sql = "SELECT * FROM writing_submission_versions WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<WritingSubmissionVersion> findBySubmissionId(UUID submissionId) {
        String sql = "SELECT * FROM writing_submission_versions WHERE submission_id = ? ORDER BY version_number DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, submissionId);
    }

    @Override
    public Optional<WritingSubmissionVersion> findBySubmissionIdAndVersionNumber(UUID submissionId, int versionNumber) {
        String sql = "SELECT * FROM writing_submission_versions WHERE submission_id = ? AND version_number = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, ROW_MAPPER, submissionId, versionNumber));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public int getNextVersionNumber(UUID submissionId) {
        String sql = "SELECT COALESCE(MAX(version_number), 0) + 1 FROM writing_submission_versions WHERE submission_id = ?";
        Integer next = jdbcTemplate.queryForObject(sql, Integer.class, submissionId);
        return next != null ? next : 1;
    }
}
