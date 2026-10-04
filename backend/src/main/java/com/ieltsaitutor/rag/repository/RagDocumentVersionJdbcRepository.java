package com.ieltsaitutor.rag.repository;

import java.time.Instant;
import java.sql.Types;
import java.time.ZoneOffset;
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
import com.ieltsaitutor.rag.embedding.EmbeddingSpace;

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
                 extraction_status, index_status, approved_at, indexed_at, created_at,
                 embedding_provider, embedding_model, embedding_dimension, embedding_version)
                VALUES (:id, :documentId, :version, :originalFilename, :mimeType, :fileSizeBytes, :checksum,
                        :storagePath, :extractionStatus, :indexStatus, :approvedAt, :indexedAt, :createdAt,
                        :embeddingProvider, :embeddingModel, :embeddingDimension, :embeddingVersion)
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
                new MapSqlParameterSource().addValue("id", id).addValue("approvedAt", approvedAt == null ? null : approvedAt.atOffset(ZoneOffset.UTC),
                        Types.TIMESTAMP_WITH_TIMEZONE));
    }

    @Override
    public void setIndexedAt(UUID id, Instant indexedAt) {
        jdbc.update("UPDATE rag_document_versions SET indexed_at = :indexedAt WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("indexedAt", indexedAt == null ? null : indexedAt.atOffset(ZoneOffset.UTC),
                        Types.TIMESTAMP_WITH_TIMEZONE));
    }

    @Override
    public void setEmbeddingSpace(UUID id, EmbeddingSpace space) {
        jdbc.update("""
                UPDATE rag_document_versions SET embedding_provider = :provider, embedding_model = :model,
                       embedding_dimension = :dimension, embedding_version = :version WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", id).addValue("provider", space.provider().name())
                .addValue("model", space.model()).addValue("dimension", space.dimension())
                .addValue("version", space.version()));
    }

    private MapSqlParameterSource params(RagDocumentVersion version) {
        return new MapSqlParameterSource()
                .addValue("id", version.id()).addValue("documentId", version.documentId())
                .addValue("version", version.version()).addValue("originalFilename", version.originalFilename())
                .addValue("mimeType", version.mimeType()).addValue("fileSizeBytes", version.fileSizeBytes())
                .addValue("checksum", version.checksum()).addValue("storagePath", version.storagePath())
                .addValue("extractionStatus", version.extractionStatus().name())
                .addValue("indexStatus", version.indexStatus().name())
                .addValue("approvedAt", version.approvedAt() == null ? null : version.approvedAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("indexedAt", version.indexedAt() == null ? null : version.indexedAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("createdAt", version.createdAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("embeddingProvider", version.embeddingSpace() == null ? null : version.embeddingSpace().provider().name())
                .addValue("embeddingModel", version.embeddingSpace() == null ? null : version.embeddingSpace().model())
                .addValue("embeddingDimension", version.embeddingSpace() == null ? null : version.embeddingSpace().dimension())
                .addValue("embeddingVersion", version.embeddingSpace() == null ? null : version.embeddingSpace().version());
    }
}
