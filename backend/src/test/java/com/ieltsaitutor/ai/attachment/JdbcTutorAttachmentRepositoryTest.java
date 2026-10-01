package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class JdbcTutorAttachmentRepositoryTest {
    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final JdbcTutorAttachmentRepository repository = new JdbcTutorAttachmentRepository(jdbc);

    @Test
    void saveAndLoadDurableMetadata() {
        repository.save(attachment());
        verify(jdbc).update(any(String.class), any(MapSqlParameterSource.class));
    }

    @Test
    void filtersByOwnerAndConversation() {
        when(jdbc.query(any(String.class), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());

        assertThat(repository.findOwned(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())).isEmpty();
        assertThat(repository.findOwnedByIds(UUID.randomUUID(), UUID.randomUUID(), List.of(UUID.randomUUID()))).isEmpty();
    }

    @Test
    void updatesLifecycleStatus() {
        when(jdbc.update(any(String.class), any(MapSqlParameterSource.class))).thenReturn(1);

        assertThat(repository.updateStatus(UUID.randomUUID(), TutorAttachment.AttachmentStatus.READY, null)).isEqualTo(1);
    }

    @Test
    void findsStaleProcessing() {
        when(jdbc.query(any(String.class), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());

        assertThat(repository.findStaleProcessing(Instant.now())).isEmpty();
    }

    @Test
    void doesNotReturnRemovedOrExpiredForChat() {
        when(jdbc.query(any(String.class), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());

        assertThat(repository.findOwnedByIds(UUID.randomUUID(), UUID.randomUUID(), List.of())).isEmpty();
    }

    private static TutorAttachment attachment() {
        Instant now = Instant.now();
        return new TutorAttachment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "notes.txt", "notes.txt",
                "text/plain", AttachmentKind.DOCUMENT, 8, "sha", "generated/one", TutorAttachment.AttachmentStatus.STORED,
                null, null, 0, now, now, null);
    }
}
