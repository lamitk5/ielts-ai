package com.ieltsaitutor.writing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class WritingAttemptStartIdempotencyTest {
    @Test
    void serializesSameUserTaskStartsBeforeCheckingForAnExistingAttempt() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.query(any(String.class), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());
        when(jdbc.update(any(String.class), any(MapSqlParameterSource.class))).thenReturn(1);

        new JdbcWritingRepository(jdbc).start(UUID.randomUUID(), "task-1-academic-01");

        verify(jdbc).query(contains("pg_advisory_xact_lock"), any(MapSqlParameterSource.class), any(RowMapper.class));
        verify(jdbc).query(contains("CAST(0 AS bigint)"), any(MapSqlParameterSource.class), any(RowMapper.class));
    }
}
