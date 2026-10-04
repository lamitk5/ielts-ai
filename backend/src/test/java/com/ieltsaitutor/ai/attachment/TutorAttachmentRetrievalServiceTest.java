package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.provider.ProviderId;
import com.ieltsaitutor.rag.embedding.EmbeddingSpace;
import com.ieltsaitutor.rag.embedding.EmbeddingVector;
import com.ieltsaitutor.rag.embedding.QueryEmbeddingService;

class TutorAttachmentRetrievalServiceTest {
    private final TutorAttachmentChunkRepository repository = mock(TutorAttachmentChunkRepository.class);
    private final QueryEmbeddingService queryEmbedding = mock(QueryEmbeddingService.class);
    private final EmbeddingSpace space = new EmbeddingSpace(ProviderId.CLOUDFLARE, "bge", 768, "v1");
    private final TutorAttachmentRetrievalService service = new TutorAttachmentRetrievalService(repository, queryEmbedding, space);

    @Test
    void filtersByOwnerConversationAndSelectedIds() {
        UUID user = UUID.randomUUID();
        UUID conversation = UUID.randomUUID();
        UUID selected = UUID.randomUUID();
        when(queryEmbedding.embedQuery("query")).thenReturn(vector(space));
        when(repository.findCandidates(user, conversation, List.of(selected), vectorValues(), space, 32)).thenReturn(
                List.of(chunk(selected, 0, .9, space)));

        assertThat(service.retrieve(user, conversation, List.of(selected), "query")).hasSize(1);
    }

    @Test
    void excludesAdminCorpusThroughPrivateRepositoryBoundary() {
        when(queryEmbedding.embedQuery("query")).thenReturn(vector(space));
        when(repository.findCandidates(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.eq(space),
                org.mockito.ArgumentMatchers.eq(32))).thenReturn(List.of());

        assertThat(service.retrieve(UUID.randomUUID(), UUID.randomUUID(), List.of(UUID.randomUUID()), "query")).isEmpty();
    }

    @Test
    void rejectsEmbeddingSpaceMismatch() {
        EmbeddingSpace other = new EmbeddingSpace(ProviderId.GEMINI, "gemini-embedding-2", 768, "v1");
        when(queryEmbedding.embedQuery("query")).thenReturn(vector(other));

        assertThatThrownBy(() -> service.retrieve(UUID.randomUUID(), UUID.randomUUID(), List.of(UUID.randomUUID()), "query"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("RAG_EMBEDDING_SPACE_MISMATCH");
    }

    @Test
    void limitsTopKToEight() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(queryEmbedding.embedQuery("query")).thenReturn(vector(space));
        when(repository.findCandidates(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.eq(space),
                org.mockito.ArgumentMatchers.eq(32))).thenReturn(java.util.stream.Stream.concat(
                        IntStream.range(0, 10).mapToObj(index -> chunk(first, index, 1.0 - index / 100.0, space)),
                        IntStream.range(0, 10).mapToObj(index -> chunk(second, index, .8 - index / 100.0, space))).toList());

        assertThat(service.retrieve(UUID.randomUUID(), UUID.randomUUID(), List.of(first, second), "query")).hasSize(8);
    }

    @Test
    void capsSingleFileAtFour() {
        UUID file = UUID.randomUUID();
        when(queryEmbedding.embedQuery("query")).thenReturn(vector(space));
        when(repository.findCandidates(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.eq(space),
                org.mockito.ArgumentMatchers.eq(32))).thenReturn(IntStream.range(0, 10)
                        .mapToObj(index -> chunk(file, index, 1.0 - index / 100.0, space)).toList());

        assertThat(service.retrieve(UUID.randomUUID(), UUID.randomUUID(), List.of(file), "query")).hasSize(4);
    }

    @Test
    void diversifiesAcrossFiles() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(queryEmbedding.embedQuery("query")).thenReturn(vector(space));
        when(repository.findCandidates(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.eq(space),
                org.mockito.ArgumentMatchers.eq(32))).thenReturn(List.of(
                        chunk(first, 0, .99, space), chunk(first, 1, .98, space), chunk(first, 2, .97, space),
                        chunk(second, 0, .96, space), chunk(second, 1, .95, space)));

        assertThat(service.retrieve(UUID.randomUUID(), UUID.randomUUID(), List.of(first, second), "query"))
                .extracting(RetrievedAttachmentChunk::attachmentId)
                .containsExactly(first, second, first, second, first);
    }

    private EmbeddingVector vector(EmbeddingSpace requested) {
        return new EmbeddingVector(768, vectorValues(), requested);
    }

    private List<Float> vectorValues() { return IntStream.range(0, 768).mapToObj(index -> 0f).toList(); }

    private RetrievedAttachmentChunk chunk(UUID attachmentId, int index, double similarity, EmbeddingSpace chunkSpace) {
        return new RetrievedAttachmentChunk(attachmentId, "file-" + attachmentId + ".txt", null, null, index,
                "content-" + index, similarity, chunkSpace);
    }
}
