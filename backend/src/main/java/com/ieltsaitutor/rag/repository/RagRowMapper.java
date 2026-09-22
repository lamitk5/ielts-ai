package com.ieltsaitutor.rag.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import com.ieltsaitutor.rag.domain.ExtractionStatus;
import com.ieltsaitutor.rag.domain.IndexStatus;
import com.ieltsaitutor.rag.domain.IngestionJobStatus;
import com.ieltsaitutor.rag.domain.RagChunk;
import com.ieltsaitutor.rag.domain.RagDocument;
import com.ieltsaitutor.rag.domain.RagDocumentVersion;
import com.ieltsaitutor.rag.domain.RagIngestionJob;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.domain.Skill;

public final class RagRowMapper {
    private RagRowMapper() {}

    public static final RowMapper<RagDocument> DOCUMENT = (rs, rowNum) -> new RagDocument(
            rs.getObject("id", UUID.class), rs.getString("title"), rs.getString("source_type"),
            rs.getString("author"), rs.getString("organization"), rs.getString("language"),
            Skill.valueOf(rs.getString("skill")), RightsStatus.valueOf(rs.getString("rights_status")),
            rs.getString("rights_note"), rs.getBoolean("active"), rs.getObject("current_version_id", UUID.class),
            rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());

    public static final RowMapper<RagDocumentVersion> VERSION = (rs, rowNum) -> new RagDocumentVersion(
            rs.getObject("id", UUID.class), rs.getObject("document_id", UUID.class), rs.getString("version"),
            rs.getString("original_filename"), rs.getString("mime_type"), rs.getLong("file_size_bytes"),
            rs.getString("checksum"), rs.getString("storage_path"),
            ExtractionStatus.valueOf(rs.getString("extraction_status")),
            IndexStatus.valueOf(rs.getString("index_status")),
            rs.getTimestamp("approved_at") == null ? null : rs.getTimestamp("approved_at").toInstant(),
            rs.getTimestamp("indexed_at") == null ? null : rs.getTimestamp("indexed_at").toInstant(),
            rs.getTimestamp("created_at").toInstant());

    public static final RowMapper<RagChunk> CHUNK = (rs, rowNum) -> new RagChunk(
            rs.getObject("id", UUID.class), rs.getObject("document_version_id", UUID.class),
            rs.getInt("chunk_index"), rs.getString("content"), (Integer) rs.getObject("page_number"),
            rs.getString("section_title"), rs.getInt("token_count"), List.of(), Map.of(),
            rs.getTimestamp("created_at").toInstant());

    public static final RowMapper<RagIngestionJob> JOB = (rs, rowNum) -> new RagIngestionJob(
            rs.getObject("id", UUID.class), rs.getObject("document_id", UUID.class),
            rs.getObject("document_version_id", UUID.class), IngestionJobStatus.valueOf(rs.getString("status")),
            rs.getString("error_code"), rs.getString("error_message"),
            rs.getTimestamp("started_at") == null ? null : rs.getTimestamp("started_at").toInstant(),
            rs.getTimestamp("finished_at") == null ? null : rs.getTimestamp("finished_at").toInstant(),
            rs.getTimestamp("created_at").toInstant());

    public static String nullableText(ResultSet rs, String column) throws SQLException {
        return rs.getString(column);
    }
}
