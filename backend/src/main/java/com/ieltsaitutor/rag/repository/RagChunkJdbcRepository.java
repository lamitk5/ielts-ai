package com.ieltsaitutor.rag.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.sql.Types;
import java.time.ZoneOffset;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ieltsaitutor.rag.domain.RagChunk;

@Repository
public class RagChunkJdbcRepository implements RagChunkRepository {
    public static final String GOVERNED_CANDIDATE_SQL = """
            SELECT c.*, d.id AS document_id, d.title AS document_title, v.version AS document_version,
                   v.original_filename AS source_id,
                   1 - (c.embedding <=> CAST(:embedding AS vector)) AS similarity
            FROM rag_chunks c
            JOIN rag_document_versions v ON v.id = c.document_version_id
            JOIN rag_documents d ON d.id = v.document_id
            WHERE d.rights_status = 'APPROVED'
              AND d.active = true
              AND v.id = d.current_version_id
              AND v.index_status = 'INDEXED'
              AND v.approved_at IS NOT NULL
              AND v.indexed_at >= v.approved_at
              AND (:skill IS NULL OR d.skill = :skill OR d.skill = 'GENERAL')
              AND (:language IS NULL OR d.language = :language)
              AND c.embedding IS NOT NULL
              AND 1 - (c.embedding <=> CAST(:embedding AS vector)) >= :minSimilarity
            ORDER BY c.embedding <=> CAST(:embedding AS vector)
            LIMIT :topK
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RagChunkJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insertBatch(List<RagChunk> chunks) {
        jdbc.batchUpdate("""
                INSERT INTO rag_chunks
                (id, document_version_id, chunk_index, content, page_number, section_title, token_count,
                 embedding, metadata, created_at)
                VALUES (:id, :documentVersionId, :chunkIndex, :content, :pageNumber, :sectionTitle, :tokenCount,
                        CAST(:embedding AS vector), CAST(:metadata AS jsonb), :createdAt)
                """, chunks.stream().map(this::params).toArray(MapSqlParameterSource[]::new));
    }

    @Override
    public void deleteForVersion(UUID versionId) {
        jdbc.update("DELETE FROM rag_chunks WHERE document_version_id = :versionId",
                new MapSqlParameterSource("versionId", versionId));
    }

    @Override
    public List<RagChunk> findGovernedCandidates(RagQueryParameters parameters) {
        return jdbc.query(GOVERNED_CANDIDATE_SQL, new MapSqlParameterSource()
                .addValue("embedding", vectorLiteral(parameters.embedding()))
                .addValue("skill", parameters.skill() == null ? null : parameters.skill().name())
                .addValue("language", parameters.language())
                .addValue("minSimilarity", parameters.minSimilarity()).addValue("topK", parameters.topK()),
                RagRowMapper.CHUNK);
    }

    private MapSqlParameterSource params(RagChunk chunk) {
        return new MapSqlParameterSource().addValue("id", chunk.id())
                .addValue("documentVersionId", chunk.documentVersionId()).addValue("chunkIndex", chunk.chunkIndex())
                .addValue("content", chunk.content()).addValue("pageNumber", chunk.pageNumber())
                .addValue("sectionTitle", chunk.sectionTitle()).addValue("tokenCount", chunk.tokenCount())
                .addValue("embedding", vectorLiteral(chunk.embedding())).addValue("metadata", "{}")
                .addValue("createdAt", chunk.createdAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);
    }

    private static String vectorLiteral(List<Float> values) {
        return values == null ? null : values.toString().replace(" ", "");
    }
}
