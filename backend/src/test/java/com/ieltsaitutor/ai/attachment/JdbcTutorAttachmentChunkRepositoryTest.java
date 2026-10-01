package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.ieltsaitutor.ai.provider.ProviderId;
import com.ieltsaitutor.rag.embedding.EmbeddingSpace;

class JdbcTutorAttachmentChunkRepositoryTest {
    @Test
    void bindsCreatedAtAsTimezoneAwareValue() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
        when(jdbc.batchUpdate(anyString(), any(MapSqlParameterSource[].class))).thenReturn(new int[] { 1 });
        JdbcTutorAttachmentChunkRepository repository = new JdbcTutorAttachmentChunkRepository(jdbc);
        EmbeddingSpace space = new EmbeddingSpace(ProviderId.GEMINI, "gemini-embedding-2", 768, "v1");
        List<Float> vector = java.util.stream.IntStream.range(0, 768).mapToObj(index -> (float) index / 768).toList();
        TutorAttachmentChunk chunk = new TutorAttachmentChunk(UUID.randomUUID(), "notes.txt", null, null, 0,
                "LUMEN attachment content", 3);

        repository.replace(chunk.attachmentId(), List.of(chunk), List.of(vector), space);

        ArgumentCaptor<MapSqlParameterSource[]> captured = ArgumentCaptor.forClass(MapSqlParameterSource[].class);
        verify(jdbc).batchUpdate(anyString(), captured.capture());
        assertThat(captured.getValue()[0].getValue("createdAt"))
                .isInstanceOf(OffsetDateTime.class);
    }
}
