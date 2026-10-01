package com.ieltsaitutor.rag.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.sql.Types;
import java.time.ZoneOffset;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ieltsaitutor.rag.domain.RagIngestionJob;

@Repository
public class RagIngestionJobJdbcRepository implements RagIngestionJobRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public RagIngestionJobJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void createJob(RagIngestionJob job) {
        jdbc.update("""
                INSERT INTO rag_ingestion_jobs
                (id, document_id, document_version_id, status, error_code, error_message, started_at, finished_at,
                 created_at)
                VALUES (:id, :documentId, :documentVersionId, :status, :errorCode, :errorMessage, :startedAt,
                        :finishedAt, :createdAt)
                """, params(job));
    }

    @Override
    public void updateStatus(RagIngestionJob job) {
        jdbc.update("""
                UPDATE rag_ingestion_jobs
                SET status = :status, error_code = :errorCode, error_message = :errorMessage,
                    started_at = :startedAt, finished_at = :finishedAt
                WHERE id = :id
                """, params(job));
    }

    @Override
    public Optional<RagIngestionJob> findById(UUID id) {
        return jdbc.query("SELECT * FROM rag_ingestion_jobs WHERE id = :id", new MapSqlParameterSource("id", id),
                RagRowMapper.JOB).stream().findFirst();
    }

    @Override
    public List<RagIngestionJob> listRecent(int limit) {
        return jdbc.query("SELECT * FROM rag_ingestion_jobs ORDER BY created_at DESC LIMIT :limit",
                new MapSqlParameterSource("limit", limit), RagRowMapper.JOB);
    }

    private MapSqlParameterSource params(RagIngestionJob job) {
        return new MapSqlParameterSource().addValue("id", job.id()).addValue("documentId", job.documentId())
                .addValue("documentVersionId", job.documentVersionId()).addValue("status", job.status().name())
                .addValue("errorCode", job.errorCode()).addValue("errorMessage", job.errorMessage())
                .addValue("startedAt", job.startedAt() == null ? null : job.startedAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("finishedAt", job.finishedAt() == null ? null : job.finishedAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("createdAt", job.createdAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);
    }
}
