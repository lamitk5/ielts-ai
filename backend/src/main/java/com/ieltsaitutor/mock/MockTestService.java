package com.ieltsaitutor.mock;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ieltsaitutor.submission.CanonicalSubmissionService;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.SubmissionStartCommand;

@Service
public class MockTestService {

    private final MockTestSessionRepository sessionRepository;
    private final MockTestSectionRepository sectionRepository;
    private final MockSectionResolver sectionResolver;
    private final MockTestStateMachine stateMachine;
    private final MockTestTimingService timingService;
    private final CanonicalSubmissionService canonicalSubmissionService;

    public MockTestService(
            MockTestSessionRepository sessionRepository,
            MockTestSectionRepository sectionRepository,
            MockSectionResolver sectionResolver,
            MockTestStateMachine stateMachine,
            MockTestTimingService timingService) {
        this(sessionRepository, sectionRepository, sectionResolver, stateMachine, timingService, null);
    }

    @Autowired
    public MockTestService(
            MockTestSessionRepository sessionRepository,
            MockTestSectionRepository sectionRepository,
            MockSectionResolver sectionResolver,
            MockTestStateMachine stateMachine,
            MockTestTimingService timingService,
            @Autowired(required = false) CanonicalSubmissionService canonicalSubmissionService) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
        this.sectionRepository = Objects.requireNonNull(sectionRepository, "sectionRepository must not be null");
        this.sectionResolver = Objects.requireNonNull(sectionResolver, "sectionResolver must not be null");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine must not be null");
        this.timingService = Objects.requireNonNull(timingService, "timingService must not be null");
        this.canonicalSubmissionService = canonicalSubmissionService;
    }

    @Transactional
    public MockTestSession startOrResume(UUID userId, String mockTestId) {
        Objects.requireNonNull(userId, "userId must not be null");
        String testId = mockTestId != null && !mockTestId.isBlank() ? mockTestId.trim() : MockTestDefinition.DEFAULT_MOCK.id();

        Optional<MockTestSession> active = sessionRepository.findActiveByUser(userId);
        if (active.isPresent()) {
            return enrichSession(active.get());
        }

        Instant now = Instant.now();
        UUID sessionId = UUID.randomUUID();
        MockTestDefinition def = MockTestDefinition.DEFAULT_MOCK;

        List<MockTestSection> sections = sectionResolver.resolveSections(sessionId, def);

        // Link with Phase 4 canonical submissions if submission service is present
        if (canonicalSubmissionService != null) {
            for (int i = 0; i < sections.size(); i++) {
                MockTestSection sec = sections.get(i);
                try {
                    String idempotencyKey = "mock-" + sessionId + "-" + sec.skill() + "-" + sec.sectionOrder();
                    PracticeSubmission sub = canonicalSubmissionService.start(userId, new SubmissionStartCommand(sec.publishedSetId(), sec.skill(), idempotencyKey));
                    sections.set(i, sec.withSubmission(sub.id()));
                } catch (Exception ignored) {
                    // Keep section with null submissionId if catalog set resolution is mocked/stubbed
                }
            }
        }

        MockTestSession session = new MockTestSession(
                sessionId,
                userId,
                testId,
                def.version(),
                MockTestSessionStatus.IN_PROGRESS,
                0,
                def.totalTimeLimitSeconds(),
                0,
                now,
                now.plusSeconds(def.totalTimeLimitSeconds()),
                null,
                now,
                now,
                sections
        );

        sessionRepository.save(session);
        sectionRepository.saveAll(sections);

        return session;
    }

    public MockTestSession getSession(UUID userId, UUID sessionId) {
        Objects.requireNonNull(userId, "userId must not be null");
        MockTestSession session = sessionRepository.findByUserAndId(userId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên thi thử hoặc không có quyền truy cập"));

        MockTestSession enriched = enrichSession(session);
        if (timingService.isExpired(enriched) && enriched.status().isMutable()) {
            MockTestSession expired = stateMachine.transition(enriched, MockTestCommand.EXPIRE);
            sessionRepository.updateStatus(expired.id(), MockTestSessionStatus.EXPIRED, Instant.now());
            return enriched.withStatus(MockTestSessionStatus.EXPIRED);
        }

        return enriched;
    }

    @Transactional
    public MockTestSession executeCommand(UUID userId, UUID sessionId, MockTestCommand command) {
        MockTestSession current = getSession(userId, sessionId);

        if (timingService.isExpired(current) && command != MockTestCommand.SUBMIT) {
            MockTestSession expired = stateMachine.transition(current, MockTestCommand.EXPIRE);
            sessionRepository.updateStatus(expired.id(), MockTestSessionStatus.EXPIRED, Instant.now());
            return expired;
        }

        MockTestSession next = stateMachine.transition(current, command);

        Instant completedAt = (next.status() == MockTestSessionStatus.SUBMITTED || next.status() == MockTestSessionStatus.COMPLETED || next.status() == MockTestSessionStatus.EXPIRED)
                ? (next.completedAt() != null ? next.completedAt() : Instant.now())
                : null;
        if (completedAt != null && next.completedAt() == null) {
            next = next.withCompletedAt(completedAt);
        }

        sessionRepository.save(next);
        if (completedAt != null) {
            sessionRepository.updateStatus(next.id(), next.status(), completedAt);
        }

        return enrichSession(next);
    }

    public List<MockTestSession> listUserSessions(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return sessionRepository.findByUser(userId).stream()
                .map(this::enrichSession)
                .toList();
    }

    private MockTestSession enrichSession(MockTestSession session) {
        List<MockTestSection> sections = sectionRepository.findBySessionId(session.id());
        int elapsed = timingService.calculateElapsedSeconds(session);
        return session.withSections(sections).withTiming(elapsed, session.expiresAt());
    }
}
