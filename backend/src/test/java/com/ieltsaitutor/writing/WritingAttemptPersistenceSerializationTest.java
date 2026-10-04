package com.ieltsaitutor.writing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class WritingAttemptPersistenceSerializationTest {
    @Test
    void persistsAssessmentWithJavaTimeCreatedAt() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.update(any(String.class), any(MapSqlParameterSource.class))).thenReturn(1);
        JdbcWritingRepository repository = new JdbcWritingRepository(jdbc);
        UUID userId = UUID.randomUUID();
        WritingAttempt attempt = new WritingAttempt(
                UUID.randomUUID(), userId, "task-1-academic-01", "TASK_1", "IN_PROGRESS",
                "A submitted response.", 3, Instant.now().minusSeconds(30), null, null);
        WritingAssessment assessment = new WritingAssessment(
                "ANSWERED", userId, attempt.taskId(), 7.0, Map.of("taskAchievement", "Strong overview"),
                java.util.List.of("Clear trends"), java.util.List.of(), java.util.List.of("Add detail"),
                java.util.List.of(), "NOT_ENABLED", false,
                "Band ước lượng — không phải điểm thi chính thức.", Instant.now(),
                attempt.responseText(), attempt.wordCount());

        assertDoesNotThrow(() -> repository.complete(attempt, assessment));
        verify(jdbc).update(contains("attempt_status='FEEDBACK_READY'"), any(MapSqlParameterSource.class));
    }
}
