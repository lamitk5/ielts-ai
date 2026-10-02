package com.ieltsaitutor.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.mock.MockConflictException;
import com.ieltsaitutor.mock.MockSectionResolver;
import com.ieltsaitutor.mock.MockTestCommand;
import com.ieltsaitutor.mock.MockTestDefinition;
import com.ieltsaitutor.mock.MockTestGradingService;
import com.ieltsaitutor.mock.MockTestResult;
import com.ieltsaitutor.mock.MockTestSection;
import com.ieltsaitutor.mock.MockTestSectionRepository;
import com.ieltsaitutor.mock.MockTestSectionStatus;
import com.ieltsaitutor.mock.MockTestService;
import com.ieltsaitutor.mock.MockTestSession;
import com.ieltsaitutor.mock.MockTestSessionRepository;
import com.ieltsaitutor.mock.MockTestSessionStatus;
import com.ieltsaitutor.mock.MockTestStateMachine;
import com.ieltsaitutor.mock.MockTestTimingService;
import com.ieltsaitutor.results.LearnerResult;
import com.ieltsaitutor.results.LearnerResultService;
import com.ieltsaitutor.results.ResultStatus;
import com.ieltsaitutor.submission.CanonicalSubmissionService;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.SubmissionStatus;

class MockTestAcceptanceTest {

    private InMemorySessionRepository sessionRepository;
    private InMemorySectionRepository sectionRepository;
    private MockSectionResolver sectionResolver;
    private MockTestStateMachine stateMachine;
    private MockTestTimingService timingService;
    private CanonicalSubmissionService submissionService;
    private MockTestService mockTestService;
    private LearnerResultService learnerResultService;
    private MockTestGradingService gradingService;

    private final UUID userA = UUID.randomUUID();
    private final UUID userB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sessionRepository = new InMemorySessionRepository();
        sectionRepository = new InMemorySectionRepository();
        sectionResolver = new MockSectionResolver();
        stateMachine = new MockTestStateMachine();
        timingService = new MockTestTimingService();
        submissionService = mock(CanonicalSubmissionService.class);

        Instant now = Instant.now();
        when(submissionService.start(any(), any())).thenAnswer(invocation -> {
            UUID owner = invocation.getArgument(0);
            return new PracticeSubmission(
                    UUID.randomUUID(), owner, "LISTENING", "mock-listening-01", "v1", "default-listening-set",
                    1, SubmissionStatus.IN_PROGRESS, now, now, null, null, 0, "start-key", null, "hash", true, now, now
            );
        });

        mockTestService = new MockTestService(
                sessionRepository, sectionRepository, sectionResolver,
                stateMachine, timingService, submissionService
        );

        learnerResultService = mock(LearnerResultService.class);
        gradingService = new MockTestGradingService(mockTestService, learnerResultService);
    }

    @Test
    void endToEndMockTestLifecycleAndSecurityIsolation() {
        // Step 1: User A starts a Mock Test session
        MockTestSession sessionA = mockTestService.startOrResume(userA, "mock-test-academic-01");
        assertNotNull(sessionA);
        assertEquals(userA, sessionA.userId());
        assertEquals(MockTestSessionStatus.IN_PROGRESS, sessionA.status());
        assertEquals(4, sessionA.sections().size());
        assertEquals(10800, sessionA.totalTimeLimitSeconds());

        // Step 2: User B cannot access User A's session
        assertThrows(IllegalArgumentException.class, () -> mockTestService.getSession(userB, sessionA.id()));
        assertThrows(IllegalArgumentException.class, () -> mockTestService.executeCommand(userB, sessionA.id(), MockTestCommand.PAUSE));

        // Step 3: User A pauses and resumes
        MockTestSession pausedA = mockTestService.executeCommand(userA, sessionA.id(), MockTestCommand.PAUSE);
        assertEquals(MockTestSessionStatus.PAUSED, pausedA.status());

        MockTestSession resumedA = mockTestService.executeCommand(userA, sessionA.id(), MockTestCommand.RESUME);
        assertEquals(MockTestSessionStatus.IN_PROGRESS, resumedA.status());

        // Step 4: User A completes sections and submits mock test
        MockTestSession submittedA = mockTestService.executeCommand(userA, sessionA.id(), MockTestCommand.SUBMIT);
        assertEquals(MockTestSessionStatus.SUBMITTED, submittedA.status());
        assertNotNull(submittedA.completedAt());

        // Cannot submit again or execute commands on completed session
        assertThrows(MockConflictException.class, () -> mockTestService.executeCommand(userA, sessionA.id(), MockTestCommand.SUBMIT));

        // Step 5: Compose results for User A
        when(learnerResultService.get(eq(userA), any())).thenReturn(
                new LearnerResult(
                        UUID.randomUUID(), "LISTENING", "mock-listening-01", "v1", "GRADED",
                        ResultStatus.READY, Instant.now(), 1800, 32, 40, BigDecimal.valueOf(0.8),
                        7.5, "Band ước lượng", "Band ước lượng bởi AI — Không phải điểm thi IELTS chính thức.",
                        null, null, List.of(), List.of(), null, List.of(), null
                )
        );

        MockTestResult resultA = gradingService.getResult(userA, sessionA.id());
        assertNotNull(resultA);
        assertEquals(sessionA.id(), resultA.sessionId());
        assertEquals(4, resultA.sectionResults().size());
        assertTrue(resultA.aiDisclaimer().contains("Không phải điểm thi IELTS chính thức"));
    }

    // In-memory repositories for test execution
    static class InMemorySessionRepository implements MockTestSessionRepository {
        private final Map<UUID, MockTestSession> storage = new ConcurrentHashMap<>();

        @Override
        public MockTestSession save(MockTestSession session) {
            storage.put(session.id(), session);
            return session;
        }

        @Override
        public Optional<MockTestSession> findById(UUID id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<MockTestSession> findByUserAndId(UUID userId, UUID id) {
            MockTestSession s = storage.get(id);
            if (s != null && s.userId().equals(userId)) return Optional.of(s);
            return Optional.empty();
        }

        @Override
        public List<MockTestSession> findByUser(UUID userId) {
            return storage.values().stream().filter(s -> s.userId().equals(userId)).toList();
        }

        @Override
        public Optional<MockTestSession> findActiveByUser(UUID userId) {
            return storage.values().stream()
                    .filter(s -> s.userId().equals(userId) && s.status().isMutable())
                    .findFirst();
        }

        @Override
        public void updateStatus(UUID id, MockTestSessionStatus status, Instant completedAt) {
            MockTestSession s = storage.get(id);
            if (s != null) {
                storage.put(id, new MockTestSession(
                        s.id(), s.userId(), s.mockTestId(), s.mockTestVersion(), status,
                        s.currentSectionIndex(), s.totalTimeLimitSeconds(), s.elapsedSeconds(),
                        s.startedAt(), s.expiresAt(), completedAt, s.createdAt(), Instant.now(), s.sections()
                ));
            }
        }
    }

    static class InMemorySectionRepository implements MockTestSectionRepository {
        private final Map<UUID, MockTestSection> storage = new ConcurrentHashMap<>();

        @Override
        public MockTestSection save(MockTestSection section) {
            storage.put(section.id(), section);
            return section;
        }

        @Override
        public List<MockTestSection> saveAll(List<MockTestSection> sections) {
            sections.forEach(s -> storage.put(s.id(), s));
            return sections;
        }

        @Override
        public List<MockTestSection> findBySessionId(UUID sessionId) {
            return storage.values().stream()
                    .filter(s -> s.sessionId().equals(sessionId))
                    .sorted((a, b) -> Integer.compare(a.sectionOrder(), b.sectionOrder()))
                    .toList();
        }

        @Override
        public Optional<MockTestSection> findBySessionIdAndOrder(UUID sessionId, int sectionOrder) {
            return storage.values().stream()
                    .filter(s -> s.sessionId().equals(sessionId) && s.sectionOrder() == sectionOrder)
                    .findFirst();
        }

        @Override
        public Optional<MockTestSection> findBySubmissionId(UUID submissionId) {
            return storage.values().stream()
                    .filter(s -> submissionId.equals(s.submissionId()))
                    .findFirst();
        }
    }
}
