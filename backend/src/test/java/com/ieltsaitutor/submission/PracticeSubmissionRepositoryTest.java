package com.ieltsaitutor.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class PracticeSubmissionRepositoryTest {
    @Test
    void createUsesJdbcCompatibleTimestampValues() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        PracticeSubmission submission = new PracticeSubmission(UUID.randomUUID(), UUID.randomUUID(), "READING",
                "reading-set-1", "reading-set-1:v3", "published-reading-1", 3, SubmissionStatus.IN_PROGRESS,
                now, now, null, null, 0, "start-1", null, null, false, now, now);

        new JdbcPracticeSubmissionRepository(jdbc).create(submission);

        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(anyString(), params.capture());
        assertInstanceOf(Timestamp.class, params.getValue().getValue("startedAt"));
        assertInstanceOf(Timestamp.class, params.getValue().getValue("createdAt"));
    }

    @Test
    void draftSaveUsesJdbcCompatibleTimestampValues() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(java.util.List.of(new SubmissionDraftSnapshot(UUID.randomUUID(), UUID.randomUUID(),
                        Map.of("q1", "B"), 1, "draft-1", now)));

        new JdbcSubmissionDraftRepository(jdbc).saveIfRevision(UUID.randomUUID(), UUID.randomUUID(),
                Map.of("q1", "B"), 0, "draft-1", now);

        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(anyString(), params.capture(), any(RowMapper.class));
        assertInstanceOf(Timestamp.class, params.getValue().getValue("updatedAt"));
    }

    @Test
    void answerSaveUsesJdbcCompatibleTimestampValues() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        UUID submissionId = UUID.randomUUID();
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(java.util.List.of(new SubmissionAnswerSnapshot(submissionId, UUID.randomUUID(),
                        Map.of("q1", "B"), "hash", now)));

        new JdbcSubmissionAnswerRepository(jdbc).save(new SubmissionAnswerSnapshot(submissionId, UUID.randomUUID(),
                Map.of("q1", "B"), "hash", now));

        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(anyString(), params.capture());
        assertInstanceOf(Timestamp.class, params.getValue().getValue("submittedAt"));
    }

    @Test
    void ownerLookupSeparatesTableNameFromWhereClause() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(java.util.List.of());

        new JdbcPracticeSubmissionRepository(jdbc)
                .findByOwnerAndStartIdempotencyKey(UUID.randomUUID(), "start-1");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        assertTrue(sql.getValue().contains("FROM practice_submissions WHERE"));
    }

    @Test
    void historyNullFiltersDeclareJdbcTypesForPostgres() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(java.util.List.of());
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), any(Class.class)))
                .thenReturn(0L);

        new JdbcPracticeSubmissionRepository(jdbc)
                .findHistory(UUID.randomUUID(), null, null, 0, 20);

        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(anyString(), params.capture(), any(RowMapper.class));
        assertEquals(Types.VARCHAR, params.getValue().getSqlType("skill"));
        assertEquals(Types.VARCHAR, params.getValue().getSqlType("status"));
    }

    @Test
    void canonicalSubmissionKeepsOwnerPinnedVersionAndInitialRevision() {
        UUID submissionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-10-01T10:00:00Z");

        PracticeSubmission submission = new PracticeSubmission(
                submissionId,
                ownerId,
                "READING",
                "reading-set-1",
                "reading-set-1:v3",
                "published-reading-1",
                3,
                SubmissionStatus.DRAFT,
                startedAt,
                startedAt,
                null,
                null,
                0L,
                null,
                null,
                null,
                true,
                startedAt,
                startedAt);

        assertEquals(submissionId, submission.id());
        assertEquals(ownerId, submission.userId());
        assertEquals("READING", submission.skill());
        assertEquals("reading-set-1:v3", submission.practiceVersionId());
        assertEquals("published-reading-1", submission.publishedSetId());
        assertEquals(3, submission.publicationRevision());
        assertEquals(SubmissionStatus.DRAFT, submission.status());
        assertEquals(0L, submission.autosaveRevision());
        assertNull(submission.submittedAt());
        assertEquals(0, submission.durationSeconds());
    }
}
