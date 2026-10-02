package com.ieltsaitutor.assessment.objective;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class QuestionResultRepositoryTest {
    @Test
    void persistsImmutableQuestionEvidenceWithJdbcCompatibleTimestamp() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        UUID user = UUID.randomUUID();
        UUID submission = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-02T10:00:00Z");
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());
        QuestionResult result = new QuestionResult(UUID.randomUUID(), submission, user, "q1", "DETAIL", "B",
                "b", "A", false, "passage:p1", "Because", "objective-v1", now);

        new JdbcQuestionResultRepository(jdbc).saveAll(List.of(result));

        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(anyString(), params.capture());
        assertEquals("passage:p1", params.getValue().getValue("evidenceReference"));
        assertInstanceOf(Timestamp.class, params.getValue().getValue("createdAt"));
    }

    @Test
    void readsOnlyOwnedSubmissionResults() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());

        new JdbcQuestionResultRepository(jdbc).findByOwnedSubmission(UUID.randomUUID(), UUID.randomUUID());

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        assertEquals(true, sql.getValue().contains("user_id = :userId AND submission_id = :submissionId"));
    }
}
