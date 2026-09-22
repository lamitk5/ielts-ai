package com.ieltsaitutor.rag.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.rag.config.RagProperties;
import com.ieltsaitutor.rag.domain.RagChunk;
import com.ieltsaitutor.rag.domain.Skill;
import com.ieltsaitutor.rag.embedding.EmbeddingVector;
import com.ieltsaitutor.rag.embedding.QueryEmbeddingService;
import com.ieltsaitutor.rag.repository.RagChunkRepository;
import com.ieltsaitutor.rag.repository.RagQueryParameters;

class VectorRetrievalServiceTest {
    private final FakeRepository repository = new FakeRepository();
    private final VectorRetrievalService service;

    VectorRetrievalServiceTest() {
        RagProperties properties = new RagProperties();
        properties.setTopK(5);
        properties.setMinSimilarity(0.72);
        service = new JdbcVectorRetrievalService(repository, query -> new EmbeddingVector(2, List.of(0.1f, 0.2f)), properties);
    }

    @BeforeEach
    void reset() { repository.chunks.clear(); }

    @Test void returnsApprovedActiveIndexedChunk() { assertThat(search(chunk("approved", .9, true))).hasSize(1); }
    @Test void excludesPendingRights() { assertThat(search(chunkWithFlag("pending", .9, false))).isEmpty(); }
    @Test void excludesRestrictedRights() { assertThat(search(chunkWithFlag("restricted", .9, false))).isEmpty(); }
    @Test void excludesInactiveDocument() { assertThat(search(chunkWithFlag("inactive", .9, false))).isEmpty(); }
    @Test void excludesNonIndexedVersion() { assertThat(search(chunkWithFlag("old", .9, false))).isEmpty(); }
    @Test void excludesOldVersionAfterReapproval() { assertThat(search(chunkWithFlag("reapproved", .9, false))).isEmpty(); }
    @Test void excludesWeakSimilarity() { assertThat(search(chunk("weak", .4, true))).isEmpty(); }

    @Test
    void respectsTopK() {
        for (int index = 0; index < 3; index++) repository.chunks.add(chunk("chunk" + index, .95, true));
        assertThat(service.search(new RagQuery("text", Skill.WRITING, "en", null, null, 2, .72))).hasSize(2);
    }

    private List<RetrievedChunk> search(RagChunk chunk) {
        repository.chunks.add(chunk);
        return service.search(new RagQuery("text", Skill.WRITING, "en", null, null, 5, .72));
    }

    private RagChunk chunk(String title, double similarity, boolean eligible) {
        return chunkWithFlag(title, similarity, eligible);
    }

    private RagChunk chunkWithFlag(String title, double similarity, boolean eligible) {
        UUID documentId = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
        return new RagChunk(UUID.randomUUID(), versionId, 0, title + " content", 2, "Section", 3, List.of(.1f, .2f),
                Map.of("documentId", documentId.toString(), "sourceId", "source-1", "title", title,
                        "version", "1", "similarity", similarity, "eligible", eligible), Instant.now());
    }

    private static final class FakeRepository implements RagChunkRepository {
        private final List<RagChunk> chunks = new ArrayList<>();
        @Override public void insertBatch(List<RagChunk> chunks) {}
        @Override public void deleteForVersion(UUID versionId) {}
        @Override public List<RagChunk> findGovernedCandidates(RagQueryParameters parameters) {
            return chunks.stream().filter(chunk -> Boolean.TRUE.equals(chunk.metadata().get("eligible")))
                    .filter(chunk -> ((Number) chunk.metadata().get("similarity")).doubleValue() >= parameters.minSimilarity())
                    .toList();
        }
    }
}
