package com.ieltsaitutor.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MockTestServiceTest {

    private MockTestSessionRepository sessionRepository;
    private MockTestSectionRepository sectionRepository;
    private MockSectionResolver sectionResolver;
    private MockTestStateMachine stateMachine;
    private MockTestTimingService timingService;
    private MockTestService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sessionRepository = mock(MockTestSessionRepository.class);
        sectionRepository = mock(MockTestSectionRepository.class);
        sectionResolver = new MockSectionResolver();
        stateMachine = new MockTestStateMachine();
        timingService = new MockTestTimingService();

        service = new MockTestService(
                sessionRepository,
                sectionRepository,
                sectionResolver,
                stateMachine,
                timingService,
                null
        );
    }

    @Test
    @DisplayName("startOrResume returns existing active session if one exists")
    void returnsActiveSessionIfExists() {
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        MockTestSession existing = new MockTestSession(
                sessionId, userId, "mock-1", "v1", MockTestSessionStatus.IN_PROGRESS,
                0, 10800, 100, now, now.plusSeconds(10800), null, now, now, List.of()
        );

        when(sessionRepository.findActiveByUser(userId)).thenReturn(Optional.of(existing));
        when(sectionRepository.findBySessionId(sessionId)).thenReturn(List.of());

        MockTestSession result = service.startOrResume(userId, "mock-1");

        assertThat(result.id()).isEqualTo(sessionId);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("startOrResume creates fresh session with 4 sections if none active")
    void createsFreshSessionWith4Sections() {
        when(sessionRepository.findActiveByUser(userId)).thenReturn(Optional.empty());

        MockTestSession result = service.startOrResume(userId, "mock-test-academic-01");

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.status()).isEqualTo(MockTestSessionStatus.IN_PROGRESS);
        assertThat(result.sections()).hasSize(4);
        assertThat(result.sections().get(0).skill()).isEqualTo("LISTENING");
        assertThat(result.sections().get(1).skill()).isEqualTo("READING");
        assertThat(result.sections().get(2).skill()).isEqualTo("WRITING");
        assertThat(result.sections().get(3).skill()).isEqualTo("SPEAKING");

        verify(sessionRepository).save(any());
        verify(sectionRepository).saveAll(any());
    }

    @Test
    @DisplayName("executeCommand transitions session status and updates repository")
    void executeCommandTransitionsSession() {
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        MockTestSession active = new MockTestSession(
                sessionId, userId, "mock-1", "v1", MockTestSessionStatus.IN_PROGRESS,
                0, 10800, 100, now, now.plusSeconds(10800), null, now, now, List.of()
        );

        when(sessionRepository.findByUserAndId(userId, sessionId)).thenReturn(Optional.of(active));
        when(sectionRepository.findBySessionId(sessionId)).thenReturn(List.of());

        MockTestSession paused = service.executeCommand(userId, sessionId, MockTestCommand.PAUSE);

        assertThat(paused.status()).isEqualTo(MockTestSessionStatus.PAUSED);
        verify(sessionRepository).save(argThat(s -> s.status() == MockTestSessionStatus.PAUSED));
    }
}
