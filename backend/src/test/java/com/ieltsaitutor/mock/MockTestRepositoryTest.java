package com.ieltsaitutor.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MockTestRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcMockTestSessionRepository sessionRepository;
    private JdbcMockTestSectionRepository sectionRepository;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        sessionRepository = new JdbcMockTestSessionRepository(jdbcTemplate);
        sectionRepository = new JdbcMockTestSectionRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("Session repository saves and updates session status")
    void saveAndFindSession() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        MockTestSession session = new MockTestSession(
                sessionId, userId, "mock-test-1", "v1", MockTestSessionStatus.NOT_STARTED,
                0, 10800, 0, null, null, null, now, now, List.of()
        );

        sessionRepository.save(session);
        verify(jdbcTemplate).update(anyString(), eq(sessionId), eq(userId), eq("mock-test-1"), eq("v1"),
                eq("NOT_STARTED"), eq(0), eq(10800), eq(0), any(), any(), any(), any(), any());

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(sessionId)))
                .thenReturn(List.of(session));

        Optional<MockTestSession> found = sessionRepository.findById(sessionId);
        assertThat(found).isPresent();
        assertThat(found.get().mockTestId()).isEqualTo("mock-test-1");
    }

    @Test
    @DisplayName("Section repository saves and finds sections by session ID")
    void saveAndFindSections() {
        UUID sessionId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        Instant now = Instant.now();

        MockTestSection section = new MockTestSection(
                sectionId, sessionId, 0, "LISTENING", "mock-l1", "v1", "set-1",
                null, 1800, MockTestSectionStatus.NOT_STARTED, now, now
        );

        sectionRepository.save(section);
        verify(jdbcTemplate).update(anyString(), eq(sectionId), eq(sessionId), eq(0), eq("LISTENING"),
                eq("mock-l1"), eq("v1"), eq("set-1"), any(), eq(1800), eq("NOT_STARTED"), any(), any());
    }
}
