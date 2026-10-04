package com.ieltsaitutor.diagnostic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.ieltsaitutor.auth.AuthException;
import com.ieltsaitutor.learning.intelligence.Skill;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.SubmissionStartCommand;

class DiagnosticSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T09:00:00Z");
    private static final String DEFINITION_VERSION = "diagnostic-v1";

    private final FakeContentSource contentSource = new FakeContentSource();
    private final FakeSessions sessions = new FakeSessions();
    private final DiagnosticContentResolver resolver =
            new DiagnosticContentResolver(contentSource, DEFINITION_VERSION);
    private final RecordingStarter starter = new RecordingStarter();
    private final DiagnosticSessionService service =
            new DiagnosticSessionService(resolver, sessions, starter, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void startingPinsAnImmutableContentSnapshotForEverySkill() {
        UUID userId = UUID.randomUUID();

        DiagnosticSession session = service.start(userId);

        assertThat(session.userId()).isEqualTo(userId);
        assertThat(session.state()).isEqualTo(DiagnosticState.IN_PROGRESS);
        assertThat(session.definitionVersion()).isEqualTo(DEFINITION_VERSION);
        assertThat(session.attemptNumber()).isEqualTo(1);
        assertThat(session.sections()).extracting(DiagnosticSectionPin::skill)
                .containsExactly(Skill.READING, Skill.LISTENING, Skill.WRITING, Skill.SPEAKING);
        assertThat(section(session, Skill.READING)).satisfies(section -> {
            assertThat(section.publishedSetId()).isEqualTo("reading-foundation-01");
            assertThat(section.practiceVersionId()).isEqualTo("11111111-1111-1111-1111-111111111111");
            assertThat(section.publicationRevision()).isEqualTo(3);
            assertThat(section.availability()).isEqualTo(DiagnosticAvailability.AVAILABLE);
            assertThat(section.started()).isFalse();
        });
        assertThat(session.startedAt()).isEqualTo(NOW);
        assertThat(session.submittedAt()).isNull();
        assertThat(session.version()).isZero();
    }

    @Test
    void aSkillWithNoApprovedContentIsLabelledUnavailableInsteadOfInvented() {
        contentSource.remove(Skill.WRITING);

        DiagnosticSession session = service.start(UUID.randomUUID());

        DiagnosticSectionPin writing = section(session, Skill.WRITING);
        assertThat(writing.availability()).isEqualTo(DiagnosticAvailability.CAPABILITY_UNAVAILABLE);
        assertThat(writing.publishedSetId()).isNull();
        assertThat(writing.practiceVersionId()).isNull();
        assertThat(writing.publicationRevision()).isZero();
        assertThat(writing.reason()).isNotBlank();
        assertThat(section(session, Skill.READING).availability()).isEqualTo(DiagnosticAvailability.AVAILABLE);
    }

    @Test
    void startingTwiceResumesTheSameSessionInsteadOfCreatingADuplicate() {
        UUID userId = UUID.randomUUID();

        DiagnosticSession first = service.start(userId);
        DiagnosticSession second = service.start(userId);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.version()).isEqualTo(first.version());
        assertThat(sessions.created).isEqualTo(1);
    }

    @Test
    void retakeCreatesANewSessionWithTheNextAttemptNumber() {
        UUID userId = UUID.randomUUID();
        DiagnosticSession first = service.start(userId);
        service.startSection(userId, first.id(), Skill.READING);
        service.submit(userId, first.id());

        DiagnosticSession retake = service.retake(userId);

        assertThat(retake.id()).isNotEqualTo(first.id());
        assertThat(retake.attemptNumber()).isEqualTo(2);
        assertThat(retake.state()).isEqualTo(DiagnosticState.IN_PROGRESS);
        assertThat(service.get(userId, first.id()).state()).isEqualTo(DiagnosticState.SUBMITTED);
    }

    @Test
    void retakeReusesThePinnedContentSoTheDiagnosticStaysComparable() {
        UUID userId = UUID.randomUUID();
        DiagnosticSession first = service.start(userId);
        service.startSection(userId, first.id(), Skill.READING);
        service.submit(userId, first.id());
        contentSource.add(Skill.READING, new DiagnosticContentPin("reading-foundation-02",
                "99999999-9999-9999-9999-999999999999", 1, "approved:newer"));

        DiagnosticSession retake = service.retake(userId);

        assertThat(section(retake, Skill.READING).publishedSetId()).isEqualTo("reading-foundation-01");
        assertThat(section(retake, Skill.READING).practiceVersionId())
                .isEqualTo("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void submitIsIdempotentAndKeepsTheFirstSubmittedAt() {
        UUID userId = UUID.randomUUID();
        DiagnosticSession session = service.start(userId);
        service.startSection(userId, session.id(), Skill.READING);

        DiagnosticSession first = service.submit(userId, session.id());
        DiagnosticSession again = service.submit(userId, session.id());

        assertThat(first.state()).isEqualTo(DiagnosticState.SUBMITTED);
        assertThat(first.submittedAt()).isEqualTo(NOW);
        assertThat(again.submittedAt()).isEqualTo(NOW);
        assertThat(again.version()).isEqualTo(first.version());
    }

    @Test
    void skippingIsAllowedAndCarriesNoInventedContent() {
        UUID userId = UUID.randomUUID();
        DiagnosticSession session = service.start(userId);

        DiagnosticSession skipped = service.skip(userId, session.id());

        assertThat(skipped.state()).isEqualTo(DiagnosticState.SKIPPED);
        assertThat(skipped.sections()).isEmpty();
        assertThat(skipped.submittedAt()).isEqualTo(NOW);
    }

    @Test
    void anotherLearnersSessionIsNeverReadableOrSubmittable() {
        UUID owner = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        DiagnosticSession session = service.start(owner);

        assertThatThrownBy(() -> service.get(attacker, session.id()))
                .isInstanceOf(AuthException.class)
                .extracting(error -> ((AuthException) error).status())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> service.submit(attacker, session.id()))
                .isInstanceOf(AuthException.class);
        assertThatThrownBy(() -> service.skip(attacker, session.id()))
                .isInstanceOf(AuthException.class);
        assertThat(service.history(attacker, 20)).isEmpty();
        assertThat(starter.started).isEmpty();
    }

    @Test
    void aMissingSessionIsReportedAsUnavailable() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service.get(userId, UUID.randomUUID()))
                .isInstanceOf(AuthException.class)
                .extracting(error -> ((AuthException) error).code())
                .isEqualTo("DIAGNOSTIC_NOT_AVAILABLE");
    }

    @Test
    void startingASectionCreatesARealPhase4SubmissionThroughTheSharedEngine() {
        UUID userId = UUID.randomUUID();
        DiagnosticSession session = service.start(userId);

        DiagnosticSectionStart started = service.startSection(userId, session.id(), Skill.READING);

        assertThat(started.submissionId()).isNotNull();
        assertThat(started.publishedSetId()).isEqualTo("reading-foundation-01");
        assertThat(starter.started).singleElement().satisfies(call -> {
            assertThat(call.ownerId()).isEqualTo(userId);
            assertThat(call.command().publishedSetId()).isEqualTo("reading-foundation-01");
            assertThat(call.command().skill()).isEqualTo("READING");
            assertThat(call.command().idempotencyKey()).startsWith("diagnostic:" + session.id() + ":READING");
        });
        assertThat(section(service.get(userId, session.id()), Skill.READING).started()).isTrue();
    }

    @Test
    void anUnavailableSectionCannotBeStarted() {
        contentSource.remove(Skill.LISTENING);
        UUID userId = UUID.randomUUID();
        DiagnosticSession session = service.start(userId);

        assertThatThrownBy(() -> service.startSection(userId, session.id(), Skill.LISTENING))
                .isInstanceOf(AuthException.class)
                .extracting(error -> ((AuthException) error).code())
                .isEqualTo("DIAGNOSTIC_CAPABILITY_UNAVAILABLE");
        assertThat(starter.started).isEmpty();
    }

    @Test
    void startingASectionTwiceReusesTheSameSubmissionThroughOneIdempotencyKey() {
        UUID userId = UUID.randomUUID();
        DiagnosticSession session = service.start(userId);

        DiagnosticSectionStart first = service.startSection(userId, session.id(), Skill.READING);
        DiagnosticSectionStart again = service.startSection(userId, session.id(), Skill.READING);

        assertThat(again.submissionId()).isEqualTo(first.submissionId());
        assertThat(starter.started).extracting(call -> call.command().idempotencyKey())
                .containsOnly("diagnostic:" + session.id() + ":READING");
        assertThat(section(service.get(userId, session.id()), Skill.READING).submissionId())
                .isEqualTo(first.submissionId());
    }

    @Test
    void aPinnedSectionCannotBeReResolvedToDifferentContent() {
        UUID userId = UUID.randomUUID();
        DiagnosticSession session = service.start(userId);
        contentSource.add(Skill.READING, new DiagnosticContentPin("reading-foundation-99",
                "88888888-8888-8888-8888-888888888888", 9, "approved:swapped"));

        DiagnosticSectionStart started = service.startSection(userId, session.id(), Skill.READING);

        assertThat(started.publishedSetId()).isEqualTo("reading-foundation-01");
    }

    @Test
    void submittingWithoutAnySectionIsRejectedRatherThanFabricated() {
        UUID userId = UUID.randomUUID();
        DiagnosticSession session = service.start(userId);

        assertThatThrownBy(() -> service.submit(userId, session.id()))
                .isInstanceOf(AuthException.class)
                .extracting(error -> ((AuthException) error).code())
                .isEqualTo("DIAGNOSTIC_INVALID_REQUEST");
        assertThat(service.get(userId, session.id()).state()).isEqualTo(DiagnosticState.IN_PROGRESS);
    }

    @Test
    void submittingIsAllowedWhenAtLeastOneSectionHasRealEvidence() {
        UUID userId = UUID.randomUUID();
        DiagnosticSession session = service.start(userId);
        service.startSection(userId, session.id(), Skill.READING);

        DiagnosticSession submitted = service.submit(userId, session.id());

        assertThat(submitted.state()).isEqualTo(DiagnosticState.SUBMITTED);
        assertThat(submitted.submittedAt()).isEqualTo(NOW);
        assertThat(submitted.sections()).extracting(DiagnosticSectionPin::started)
                .containsExactly(true, false, false, false);
    }

    @Test
    void theOwnerHistoryIsBoundedAndNewestFirst() {
        UUID userId = UUID.randomUUID();
        service.start(userId);
        DiagnosticSession first = service.start(userId);
        service.startSection(userId, first.id(), Skill.READING);
        service.submit(userId, first.id());
        service.retake(userId);
        service.retake(userId);

        List<DiagnosticSession> history = service.history(userId, 2);

        assertThat(history).hasSize(2);
        assertThat(history).extracting(DiagnosticSession::attemptNumber).containsExactly(3, 2);
        assertThat(history).extracting(DiagnosticSession::userId).containsOnly(userId);
    }

    private DiagnosticSectionPin section(DiagnosticSession session, Skill skill) {
        return session.sections().stream().filter(pin -> pin.skill() == skill).findFirst().orElseThrow();
    }

    private record StartedCall(UUID ownerId, SubmissionStartCommand command) {
    }

    private static final class RecordingStarter implements DiagnosticSubmissionStarter {
        private final List<StartedCall> started = new ArrayList<>();
        private final Map<String, UUID> byKey = new LinkedHashMap<>();

        @Override
        public UUID start(UUID ownerId, SubmissionStartCommand command) {
            started.add(new StartedCall(ownerId, command));
            return byKey.computeIfAbsent(command.idempotencyKey(), key -> UUID.randomUUID());
        }
    }

    private static final class FakeContentSource implements DiagnosticContentSource {
        private final Map<Skill, DiagnosticContentPin> approved = new LinkedHashMap<>();

        private FakeContentSource() {
            add(Skill.READING, new DiagnosticContentPin("reading-foundation-01",
                    "11111111-1111-1111-1111-111111111111", 3, "approved:phase-3"));
            add(Skill.LISTENING, new DiagnosticContentPin("listening-foundation-01",
                    "22222222-2222-2222-2222-222222222222", 2, "approved:phase-3"));
            add(Skill.WRITING, new DiagnosticContentPin("writing-foundation-01",
                    "33333333-3333-3333-3333-333333333333", 1, "approved:phase-3"));
            add(Skill.SPEAKING, new DiagnosticContentPin("speaking-foundation-01",
                    "44444444-4444-4444-4444-444444444444", 1, "approved:phase-3"));
        }

        private void add(Skill skill, DiagnosticContentPin pin) { approved.put(skill, pin); }

        private void remove(Skill skill) { approved.remove(skill); }

        @Override
        public Optional<DiagnosticContentPin> newestApproved(Skill skill) {
            return Optional.ofNullable(approved.get(skill));
        }
    }

    private static final class FakeSessions implements DiagnosticSessionRepository {
        private final Map<UUID, DiagnosticSession> rows = new LinkedHashMap<>();
        private int created;

        @Override
        public DiagnosticSession create(DiagnosticSession session) {
            rows.put(session.id(), session);
            created++;
            return session;
        }

        @Override
        public Optional<DiagnosticSession> find(UUID userId, UUID sessionId) {
            DiagnosticSession session = rows.get(sessionId);
            return session != null && session.userId().equals(userId) ? Optional.of(session) : Optional.empty();
        }

        @Override
        public Optional<DiagnosticSession> findActive(UUID userId) {
            return rows.values().stream()
                    .filter(session -> session.userId().equals(userId))
                    .filter(session -> session.state() == DiagnosticState.IN_PROGRESS)
                    .findFirst();
        }

        @Override
        public boolean update(DiagnosticSession session, long expectedVersion) {
            DiagnosticSession current = rows.get(session.id());
            if (current == null || current.version() != expectedVersion) {
                return false;
            }
            rows.put(session.id(), session);
            return true;
        }

        @Override
        public List<DiagnosticSession> history(UUID userId, int limit) {
            return rows.values().stream()
                    .filter(session -> session.userId().equals(userId))
                    .sorted((left, right) -> Integer.compare(right.attemptNumber(), left.attemptNumber()))
                    .limit(limit)
                    .toList();
        }

        @Override
        public int nextAttemptNumber(UUID userId) {
            return rows.values().stream()
                    .filter(session -> session.userId().equals(userId))
                    .mapToInt(DiagnosticSession::attemptNumber)
                    .max()
                    .orElse(0) + 1;
        }
    }
}
