package com.ieltsaitutor.rag.retrieval;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ieltsaitutor.rag.config.RagProperties;
import com.ieltsaitutor.rag.domain.RagChunk;
import com.ieltsaitutor.rag.embedding.EmbeddingVector;
import com.ieltsaitutor.rag.embedding.EmbeddingSpace;
import com.ieltsaitutor.rag.embedding.QueryEmbeddingService;
import com.ieltsaitutor.rag.repository.RagChunkRepository;
import com.ieltsaitutor.rag.repository.RagQueryParameters;

@Service
public class JdbcVectorRetrievalService implements VectorRetrievalService {
    private final RagChunkRepository chunks;
    private final QueryEmbeddingService queryEmbeddings;
    private final RagProperties properties;

    public JdbcVectorRetrievalService(RagChunkRepository chunks, QueryEmbeddingService queryEmbeddings,
            RagProperties properties) {
        this.chunks = chunks;
        this.queryEmbeddings = queryEmbeddings;
        this.properties = properties;
    }

    @Override
    public List<RetrievedChunk> search(RagQuery query) {
        EmbeddingVector embedding = queryEmbeddings.embedQuery(query.text());
        EmbeddingSpace space = query.space() != null ? query.space() : embedding.space();
        int topK = query.topK() > 0 ? query.topK() : properties.topK();
        double minSimilarity = query.minSimilarity() > 0 ? query.minSimilarity() : properties.minSimilarity();
        List<RagChunk> candidates = chunks.findGovernedCandidates(new RagQueryParameters(embedding.values(), query.skill(),
                query.language(), topK, minSimilarity, space));
        return candidates.stream().map(this::map).filter(result -> result.similarity() >= minSimilarity)
                .filter(result -> space == null || result.space() == null || result.space().matches(space))
                .sorted(Comparator.comparingDouble(RetrievedChunk::similarity).reversed()).limit(topK).toList();
    }

    private RetrievedChunk map(RagChunk chunk) {
        Map<String, Object> metadata = chunk.metadata() == null ? Map.of() : chunk.metadata();
        return new RetrievedChunk(chunk.id(), uuid(metadata.get("documentId")), chunk.documentVersionId(),
                text(metadata.get("sourceId")), text(metadata.get("title")), text(metadata.get("version")),
                chunk.pageNumber(), chunk.sectionTitle(), chunk.content(), number(metadata.get("similarity")),
                space(metadata));
    }

    private UUID uuid(Object value) { return value == null ? null : UUID.fromString(value.toString()); }
    private String text(Object value) { return value == null ? null : value.toString(); }
    private double number(Object value) { return value instanceof Number number ? number.doubleValue() : 0d; }

    private EmbeddingSpace space(Map<String, Object> metadata) {
        Object value = metadata.get("embeddingSpace");
        return value instanceof EmbeddingSpace embeddingSpace ? embeddingSpace : null;
    }
}
