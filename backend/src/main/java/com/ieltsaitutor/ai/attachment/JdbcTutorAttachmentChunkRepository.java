package com.ieltsaitutor.ai.attachment;

import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ieltsaitutor.ai.provider.ProviderId;
import com.ieltsaitutor.rag.embedding.EmbeddingSpace;

@Repository
public class JdbcTutorAttachmentChunkRepository implements TutorAttachmentChunkRepository {
    private static final RowMapper<RetrievedAttachmentChunk> MAPPER = (rs, row) -> {
        String provider = rs.getString("embedding_provider");
        String model = rs.getString("embedding_model");
        Integer dimension = (Integer) rs.getObject("embedding_dimension");
        String version = rs.getString("embedding_version");
        EmbeddingSpace space = provider == null || model == null || dimension == null || version == null ? null
                : new EmbeddingSpace(ProviderId.valueOf(provider), model, dimension, version);
        return new RetrievedAttachmentChunk(rs.getObject("attachment_id", UUID.class), rs.getString("original_filename"),
                (Integer) rs.getObject("page_number"), rs.getString("section_label"), rs.getInt("chunk_index"),
                rs.getString("content"), rs.getDouble("similarity"), space);
    };

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcTutorAttachmentChunkRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void replace(UUID attachmentId, List<TutorAttachmentChunk> chunks, List<List<Float>> embeddings,
            EmbeddingSpace space) {
        if (chunks == null || (embeddings != null && chunks.size() != embeddings.size())) {
            throw new IllegalArgumentException("Attachment chunks and embeddings must align");
        }
        jdbc.update("DELETE FROM ai_attachment_chunks WHERE attachment_id=:attachmentId",
                new MapSqlParameterSource("attachmentId", attachmentId));
        if (chunks.isEmpty()) return;
        MapSqlParameterSource[] parameters = new MapSqlParameterSource[chunks.size()];
        for (int index = 0; index < chunks.size(); index++) {
            TutorAttachmentChunk chunk = chunks.get(index);
            List<Float> embedding = embeddings == null ? null : embeddings.get(index);
            if (embeddings != null && (embedding == null || embedding.size() != 768)) {
                throw new IllegalArgumentException("Attachment embeddings must use vector(768)");
            }
            parameters[index] = new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID())
                    .addValue("attachmentId", attachmentId)
                    .addValue("chunkIndex", chunk.chunkIndex())
                    .addValue("pageNumber", chunk.pageNumber())
                    .addValue("sectionLabel", chunk.sectionLabel())
                    .addValue("content", chunk.content())
                    .addValue("tokenEstimate", chunk.tokenEstimate())
                    .addValue("embedding", embedding == null ? null : vectorLiteral(embedding))
                    .addValue("embeddingProvider", space == null ? null : space.provider().name())
                    .addValue("embeddingModel", space == null ? null : space.model())
                    .addValue("embeddingDimension", space == null ? null : space.dimension())
                    .addValue("embeddingVersion", space == null ? null : space.version())
                    .addValue("createdAt", Instant.now().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);
        }
        jdbc.batchUpdate("""
                INSERT INTO ai_attachment_chunks
                    (id, attachment_id, chunk_index, page_number, section_label, content, token_estimate,
                     embedding, embedding_provider, embedding_model, embedding_dimension, embedding_version, created_at)
                VALUES (:id, :attachmentId, :chunkIndex, :pageNumber, :sectionLabel, :content, :tokenEstimate,
                        CAST(:embedding AS vector), :embeddingProvider, :embeddingModel, :embeddingDimension,
                        :embeddingVersion, :createdAt)
                """, parameters);
    }

    @Override
    public List<RetrievedAttachmentChunk> findByAttachmentId(UUID attachmentId) {
        return jdbc.query("""
                SELECT c.attachment_id, a.original_filename, c.page_number, c.section_label, c.chunk_index,
                       content, 0d AS similarity, embedding_provider, embedding_model, embedding_dimension,
                       embedding_version
                FROM ai_attachment_chunks c
                JOIN ai_attachments a ON a.id = c.attachment_id
                WHERE c.attachment_id = :attachmentId
                ORDER BY c.chunk_index
                """, new MapSqlParameterSource("attachmentId", attachmentId), MAPPER);
    }

    @Override
    public List<RetrievedAttachmentChunk> findCandidates(UUID userId, UUID conversationId, List<UUID> attachmentIds,
            List<Float> queryEmbedding, EmbeddingSpace space, int limit) {
        if (attachmentIds == null || attachmentIds.isEmpty()) return List.of();
        return jdbc.query("""
                SELECT c.attachment_id, a.original_filename, c.page_number, c.section_label, c.chunk_index,
                       c.content, 1 - (c.embedding <=> CAST(:embedding AS vector)) AS similarity,
                       c.embedding_provider, c.embedding_model, c.embedding_dimension, c.embedding_version
                FROM ai_attachment_chunks c
                JOIN ai_attachments a ON a.id = c.attachment_id
                WHERE a.owner_user_id = :userId
                  AND a.conversation_id = :conversationId
                  AND a.status = 'READY'
                  AND c.attachment_id IN (:attachmentIds)
                  AND c.embedding_provider = :embeddingProvider
                  AND c.embedding_model = :embeddingModel
                  AND c.embedding_dimension = :embeddingDimension
                  AND c.embedding_version = :embeddingVersion
                  AND c.embedding IS NOT NULL
                ORDER BY c.embedding <=> CAST(:embedding AS vector)
                LIMIT :limit
                """, new MapSqlParameterSource()
                .addValue("userId", userId).addValue("conversationId", conversationId)
                .addValue("attachmentIds", attachmentIds).addValue("embedding", vectorLiteral(queryEmbedding))
                .addValue("embeddingProvider", space.provider().name()).addValue("embeddingModel", space.model())
                .addValue("embeddingDimension", space.dimension()).addValue("embeddingVersion", space.version())
                .addValue("limit", limit), MAPPER);
    }

    private static String vectorLiteral(List<Float> values) {
        return "[" + values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + "]";
    }
}
