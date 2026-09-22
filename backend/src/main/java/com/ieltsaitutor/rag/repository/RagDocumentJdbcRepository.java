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

import com.ieltsaitutor.rag.domain.RagDocument;
import com.ieltsaitutor.rag.domain.RightsStatus;

@Repository
public class RagDocumentJdbcRepository implements RagDocumentRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public RagDocumentJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void create(RagDocument document) {
        jdbc.update("""
                INSERT INTO rag_documents
                (id, title, source_type, author, organization, language, skill, rights_status, rights_note,
                 active, current_version_id, created_at, updated_at)
                VALUES (:id, :title, :sourceType, :author, :organization, :language, :skill, :rightsStatus,
                        :rightsNote, :active, :currentVersionId, :createdAt, :updatedAt)
                """, params(document));
    }

    @Override
    public Optional<RagDocument> findById(UUID id) {
        return jdbc.query("SELECT * FROM rag_documents WHERE id = :id", new MapSqlParameterSource("id", id),
                RagRowMapper.DOCUMENT).stream().findFirst();
    }

    @Override
    public List<RagDocument> listSummaries() {
        return jdbc.query("SELECT * FROM rag_documents ORDER BY updated_at DESC", Map.of(), RagRowMapper.DOCUMENT);
    }

    @Override
    public void updateRightsStatus(UUID id, RightsStatus status, String rightsNote) {
        jdbc.update("UPDATE rag_documents SET rights_status = :status, rights_note = :rightsNote, "
                + "updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("status", status.name())
                        .addValue("rightsNote", rightsNote));
    }

    @Override
    public void activate(UUID id) {
        jdbc.update("UPDATE rag_documents SET active = true, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    @Override
    public void deactivate(UUID id) {
        jdbc.update("UPDATE rag_documents SET active = false, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    @Override
    public void setCurrentVersion(UUID id, UUID versionId) {
        jdbc.update("UPDATE rag_documents SET current_version_id = :versionId, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = :id", new MapSqlParameterSource().addValue("id", id).addValue("versionId", versionId));
    }

    private MapSqlParameterSource params(RagDocument document) {
        return new MapSqlParameterSource()
                .addValue("id", document.id()).addValue("title", document.title())
                .addValue("sourceType", document.sourceType()).addValue("author", document.author())
                .addValue("organization", document.organization()).addValue("language", document.language())
                .addValue("skill", document.skill().name()).addValue("rightsStatus", document.rightsStatus().name())
                .addValue("rightsNote", document.rightsNote()).addValue("active", document.active())
                .addValue("currentVersionId", document.currentVersionId())
                .addValue("createdAt", document.createdAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("updatedAt", document.updatedAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);
    }
}
