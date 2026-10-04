package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.ieltsaitutor.ai.provider.ProviderId;
import com.ieltsaitutor.rag.embedding.EmbeddingSpace;

class TutorAttachmentChunkRepositoryTest {
    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final JdbcTutorAttachmentChunkRepository repository = new JdbcTutorAttachmentChunkRepository(jdbc);
    private final EmbeddingSpace space = new EmbeddingSpace(ProviderId.CLOUDFLARE, "bge", 768, "v1");

    @Test
    void persistsPrivateChunksWithEmbeddingMetadata() {
        repository.replace(UUID.randomUUID(), List.of(chunk()),
                List.of(java.util.stream.IntStream.range(0, 768).mapToObj(index -> 0.1f).toList()), space);

        verify(jdbc).batchUpdate(anyString(), any(MapSqlParameterSource[].class));
    }

    @Test
    void persistsPrivateChunksWithoutEmbeddingMetadata() {
        UUID attachmentId = UUID.randomUUID();
        repository.replace(attachmentId, List.of(chunk()), null, null);

        org.mockito.ArgumentCaptor<MapSqlParameterSource[]> captured =
                org.mockito.ArgumentCaptor.forClass(MapSqlParameterSource[].class);
        verify(jdbc).batchUpdate(anyString(), captured.capture());
        assertThat(captured.getValue()[0].getValue("embedding")).isNull();
        assertThat(captured.getValue()[0].getValue("embeddingProvider")).isNull();
        assertThat(captured.getValue()[0].getValue("embeddingModel")).isNull();
        assertThat(captured.getValue()[0].getValue("embeddingDimension")).isNull();
        assertThat(captured.getValue()[0].getValue("embeddingVersion")).isNull();
    }

    @Test
    void retrievesOnlyGovernedPrivateCandidates() {
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());

        assertThat(repository.findCandidates(UUID.randomUUID(), UUID.randomUUID(), List.of(UUID.randomUUID()),
                List.of(0.1f, 0.2f), space, 8)).isEmpty();
    }

    private TutorAttachmentChunk chunk() {
        return new TutorAttachmentChunk(UUID.randomUUID(), "notes.txt", 1, "Reading", 0, "content", 2);
    }
}
