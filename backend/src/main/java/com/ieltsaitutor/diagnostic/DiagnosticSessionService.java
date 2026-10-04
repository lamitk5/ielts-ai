package com.ieltsaitutor.diagnostic;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.auth.AuthException;
import com.ieltsaitutor.learning.intelligence.Skill;
import com.ieltsaitutor.submission.SubmissionStartCommand;

/**
 * Owns the diagnostic session lifecycle: start, resume, retake, submit and skip.
 *
 * <p>The session is owner-scoped, the pinned content snapshot is immutable, and
 * no transition can invent evidence: submitting requires at least one section
 * that was actually answered through the Phase 4 engine.
 */
@Service
public class DiagnosticSessionService {
    static final int MAX_HISTORY = 20;

    private final DiagnosticContentResolver resolver;
    private final DiagnosticSessionRepository repository;
    private final DiagnosticSubmissionStarter starter;
    private final Clock clock;

    @Autowired
    public DiagnosticSessionService(DiagnosticContentResolver resolver, DiagnosticSessionRepository repository,
            DiagnosticSubmissionStarter starter) {
        this(resolver, repository, starter, Clock.systemUTC());
    }

    public DiagnosticSessionService(DiagnosticContentResolver resolver, DiagnosticSessionRepository repository,
            DiagnosticSubmissionStarter starter, Clock clock) {
        this.resolver = resolver;
        this.repository = repository;
        this.starter = starter;
        this.clock = clock;
    }

    public DiagnosticSession start(UUID ownerId) {
        requireOwner(ownerId);
        return repository.findActive(ownerId).orElseGet(() -> {
            Instant now = now();
            return repository.create(new DiagnosticSession(UUID.randomUUID(), ownerId, DiagnosticState.IN_PROGRESS,
                    resolver.definitionVersion(), repository.nextAttemptNumber(ownerId), resolver.resolveSnapshot(),
                    now, null, 0L, now, now));
        });
    }

    public DiagnosticSession get(UUID ownerId, UUID sessionId) {
        requireOwner(ownerId);
        return repository.find(ownerId, sessionId).orElseThrow(DiagnosticSessionService::notAvailable);
    }

    public DiagnosticSession retake(UUID ownerId) {
        requireOwner(ownerId);
        Instant now = now();
        List<DiagnosticSectionPin> pinned = repository.history(ownerId, 1).stream().findFirst()
                .map(previous -> previous.sections().stream().map(DiagnosticSectionPin::resetForRetake).toList())
                .orElseGet(resolver::resolveSnapshot);
        return repository.create(new DiagnosticSession(UUID.randomUUID(), ownerId, DiagnosticState.IN_PROGRESS,
                resolver.definitionVersion(), repository.nextAttemptNumber(ownerId), pinned,
                now, null, 0L, now, now));
    }

    public List<DiagnosticSession> history(UUID ownerId, int limit) {
        requireOwner(ownerId);
        return repository.history(ownerId, Math.max(1, Math.min(limit, MAX_HISTORY)));
    }

    public DiagnosticSectionStart startSection(UUID ownerId, UUID sessionId, Skill skill) {
        DiagnosticSession session = get(ownerId, sessionId);
        DiagnosticSectionPin section = session.section(skill);
        if (!section.isAnswerable()) {
            throw new AuthException("DIAGNOSTIC_CAPABILITY_UNAVAILABLE", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Phần này chưa có nội dung khả dụng.");
        }
        SubmissionStartCommand command = new SubmissionStartCommand(section.publishedSetId(), skill.name(),
                idempotencyKey(session, skill));
        UUID submissionId = starter.start(ownerId, command);
        persist(session.withSections(replace(session, section.withSubmission(submissionId)), now()));
        return new DiagnosticSectionStart(skill.name(), submissionId, section.publishedSetId(),
                section.practiceVersionId(), section.publicationRevision());
    }

    public DiagnosticSession submit(UUID ownerId, UUID sessionId) {
        DiagnosticSession session = get(ownerId, sessionId);
        if (session.isFinalized()) {
            return session;
        }
        if (!session.hasEvidence()) {
            throw new AuthException("DIAGNOSTIC_INVALID_REQUEST", HttpStatus.BAD_REQUEST,
                    "Bạn chưa trả lời phần nào nên chưa thể tạo kết quả chẩn đoán.");
        }
        return persist(session.withState(DiagnosticState.SUBMITTED, now(), now()));
    }

    public DiagnosticSession skip(UUID ownerId, UUID sessionId) {
        DiagnosticSession session = get(ownerId, sessionId);
        if (session.isFinalized()) {
            return session;
        }
        Instant now = now();
        return persist(session.withStateAndSections(DiagnosticState.SKIPPED, List.of(), now, now));
    }

    private DiagnosticSession persist(DiagnosticSession session) {
        if (!repository.update(session, session.version() - 1)) {
            throw new AuthException("DIAGNOSTIC_VERSION_CONFLICT", HttpStatus.CONFLICT,
                    "Chẩn đoán đã được thay đổi ở nơi khác. Vui lòng tải lại.");
        }
        return session;
    }

    private List<DiagnosticSectionPin> replace(DiagnosticSession session, DiagnosticSectionPin updated) {
        List<DiagnosticSectionPin> sections = new ArrayList<>(session.sections());
        for (int index = 0; index < sections.size(); index++) {
            if (sections.get(index).skill() == updated.skill()) {
                sections.set(index, updated);
            }
        }
        return sections;
    }

    private String idempotencyKey(DiagnosticSession session, Skill skill) {
        return "diagnostic:" + session.id() + ":" + skill.name();
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private static void requireOwner(UUID ownerId) {
        if (ownerId == null) {
            throw new AuthException("AUTH_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
        }
    }

    private static AuthException notAvailable() {
        return new AuthException("DIAGNOSTIC_NOT_AVAILABLE", HttpStatus.NOT_FOUND,
                "Chẩn đoán không khả dụng.");
    }
}
