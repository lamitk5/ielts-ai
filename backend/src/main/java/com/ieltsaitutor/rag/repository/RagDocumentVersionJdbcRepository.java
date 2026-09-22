package com.ieltsaitutor.rag.repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ieltsaitutor.rag.domain.ExtractionStatus;
import com.ieltsaitutor.rag.domain.IndexStatus;
import com.ieltsaitutor.rag.domain.RagDocumentVersion;

@Repository
public class RagDocumentVersionJdbcRepository implements RagDocumentVersionRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public RagDocumentVersionJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void createVersion(RagDocumentVersion version) {
        jdbc.update("""
                INSERT INTO rag_document_versions
                (id, document_id, version, original_filename, mime_type, file_size_bytes, checksum, storage_path,
                 extraction_status, index_status, approved_at, indexed_at, created_at)
                VALUES (:id, :documentId, :version, :originalFilename, :mimeType, :fileSizeBytes, :checksum,
                        :storagePath, :extractionStatus, :indexStatus, :approvedAt, :indexedAt, :createdAt)
                """, params(version));
    }

    @Override
    public Optional<RagDocumentVersion> findById(UUID id) {
        return jdbc.query("SELECT * FROM rag_document_versions WHERE id = :id", new MapSqlParameterSource("id", id),
                RagRowMapper.VERSION).stream().findFirst();
    }

    @Override
    public Optional<RagDocumentVersion> findCurrent(UUID documentId) {
        return jdbc.query("""
                SELECT v.* FROM rag_document_versions v
                JOIN rag_documents d ON d.current_version_id = v.id
                WHERE d.id = :documentId
                """, new MapSqlParameterSource("documentId", documentId), RagRowMapper.VERSION).stream().findFirst();
    }

    @Override
    public Optional<RagDocumentVersion> findByChecksum(String checksum) {
        return jdbc.query("SELECT * FROM rag_document_versions WHERE checksum = :checksum",
                new MapSqlParameterSource("checksum", checksum), RagRowMapper.VERSION).stream().findFirst();
    }

    @Override
    public void updateExtractionStatus(UUID id, ExtractionStatus status) {
        jdbc.update("UPDATE rag_document_versions SET extraction_status = :status WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("status", status.name()));
    }

    @Override
    public void updateIndexStatus(UUID id, IndexStatus status) {
        jdbc.update("UPDATE rag_document_versions SET index_status = :status WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("status", status.name()));
    }

    @Override
    public void setApprovedAt(UUID id, Instant approvedAt) {
        jdbc.update("UPDATE rag_document_versions SET approved_at = :approvedAt WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("approvedAt", approvedAt));
    }

    @Override
    public void setIndexedAt(UUID id, Instant indexedAt) {
        jdbc.update("UPDATE rag_document_versions SET indexed_at = :indexedAt WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("indexedAt", indexedAt));
    }

    private MapSqlParameterSource params(RagDocumentVersion version) {
        return new MapSqlParameterSource()
                .addValue("id", version.id()).addValue("documentId", version.documentId())
                .addValue("version", version.version()).addValue("originalFilename", version.originalFilename())
                .addValue("mimeType", version.mimeType()).addValue("fileSizeBytes", version.fileSizeBytes())
                .addValue("checksum", version.checksum()).addValue("storagePath", version.storagePath())
                .addValue("extractionStatus", version.extractionStatus().name())
                .addValue("indexStatus", version.indexStatus().name()).addValue("approvedAt", version.approvedAt())
                .addValue("indexedAt", version.indexedAt()).addValue("createdAt", version.createdAt());
    }
}
