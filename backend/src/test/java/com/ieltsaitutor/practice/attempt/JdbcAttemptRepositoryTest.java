package com.ieltsaitutor.practice.attempt;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class JdbcAttemptRepositoryTest {
    @Test
    void createUsesJdbcCompatibleOffsetDateTimeForTimestampWithTimezone() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
        JdbcAttemptRepository repository = new JdbcAttemptRepository(jdbc);

        repository.create(UUID.randomUUID(), "reading-foundation-01", "reading-foundation-01:v1", "reading", "qa-key");

        var parameters = org.mockito.ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(anyString(), parameters.capture());
        assertInstanceOf(OffsetDateTime.class, parameters.getValue().getValue("createdAt"));
    }

    @Test
    void resultPersistenceUsesJdbcCompatibleOffsetDateTimeForTimestampWithTimezone() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
        JdbcAttemptRepository repository = new JdbcAttemptRepository(jdbc);
        PracticeAttempt attempt = new PracticeAttempt(UUID.randomUUID(), UUID.randomUUID(), "reading-foundation-01", "reading-foundation-01:v1",
                "reading", AttemptStatus.IN_PROGRESS, Map.of(), null, null, Instant.now(), null, "{}", "qa-key");

        repository.saveSubmitted(attempt, Map.of("reading-q1", "B"));
        repository.saveResult(attempt, Map.of("reading-q1", "B"), 1, 1, "{}");

        var parameters = org.mockito.ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc, times(2)).update(anyString(), parameters.capture());
        parameters.getAllValues().forEach(value -> assertInstanceOf(OffsetDateTime.class, value.getValue("submittedAt")));
    }

    @Test
    void submittingAnAttemptDoesNotWriteNullIntoRequiredScoreColumns() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
        JdbcAttemptRepository repository = new JdbcAttemptRepository(jdbc);
        PracticeAttempt attempt = new PracticeAttempt(UUID.randomUUID(), UUID.randomUUID(), "reading-foundation-01", "reading-foundation-01:v1",
                "reading", AttemptStatus.IN_PROGRESS, Map.of(), 0, 1, Instant.now(), null, "{}", "qa-key");

        repository.saveSubmitted(attempt, Map.of("reading-q1", "B"));

        var sql = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), any(MapSqlParameterSource.class));
        assertFalse(sql.getValue().contains("score = NULL"));
        assertFalse(sql.getValue().contains("total = NULL"));
    }
}
