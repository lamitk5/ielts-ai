package com.ieltsaitutor.tutor.context;

import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.learning.LearningAttempt;
import com.ieltsaitutor.learning.LearningRepository;
import com.ieltsaitutor.learning.LearningActivity;
import com.ieltsaitutor.practice.SyntheticPracticeCatalog;
import com.ieltsaitutor.practice.PracticeAttemptSnapshot;
import com.ieltsaitutor.practice.PracticeAttemptStore;
import com.ieltsaitutor.speaking.SpeakingAttempt;
import com.ieltsaitutor.speaking.SpeakingRepository;
import com.ieltsaitutor.writing.WritingAssessment;
import com.ieltsaitutor.writing.WritingRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TutorContextServiceTest {
    private final UUID owner = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final FakeLearningRepository learning = new FakeLearningRepository();
    private final FakeWritingRepository writing = new FakeWritingRepository();
    private final FakeSpeakingRepository speaking = new FakeSpeakingRepository();
    private final FakePracticeAttemptStore attempts = new FakePracticeAttemptStore();
    private final TutorContextService service = new DefaultTutorContextService(
            SyntheticPracticeCatalog.inMemory(), learning, writing, speaking, attempts);

    @Test
    void resolvesTrustedReadingQuestionAndIgnoresForgedClientFields() {
        attempts.latest = new PracticeAttemptSnapshot(UUID.randomUUID(), owner, "reading", "reading-foundation-01",
                1, 2, java.util.Map.of("reading-q1", "C"), Instant.now());
        TutorLearningContext context = service.resolve(principal(owner), new TutorContextRequest(
                "reading", "reading-foundation-01", "reading-q1", null, null, null));

        assertThat(context.available()).isTrue();
        assertThat(context.correctAnswer()).isEqualTo("B");
        assertThat(context.explanation()).contains("recall");
        assertThat(context.selectedAnswer()).isEqualTo("C");
        assertThat(context.score()).isEqualTo(1);
        assertThat(context).extracting(TutorLearningContext::rawClientData).isEqualTo(null);
    }

    @Test
    void resolvesOwnedWritingAndSpeakingRecordsOnly() {
        writing.items.add(WritingAssessment.unavailable(owner, "task-1"));
        speaking.items.add(new SpeakingAttempt(UUID.randomUUID(), owner, "speaking-p1-01", "answer", null,
                "INPUT_SAVED", null, Instant.now()));
        speaking.items.add(new SpeakingAttempt(UUID.randomUUID(), other, "speaking-p1-01", "private", null,
                "INPUT_SAVED", null, Instant.now()));

        TutorLearningContext writingContext = service.resolve(principal(owner),
                new TutorContextRequest("writing", null, null, null, "task-1", null));
        TutorLearningContext speakingContext = service.resolve(principal(owner),
                new TutorContextRequest("speaking", null, null, null, null, "speaking-p1-01"));

        assertThat(writingContext.writingTaskId()).isEqualTo("task-1");
        assertThat(speakingContext.speakingTranscript()).isEqualTo("answer");
        assertThat(speakingContext.speakingTranscript()).doesNotContain("private");
    }

    @Test
    void unauthenticatedPrivateContextIsRejectedAndCrossUserDataIsAbsent() {
        assertThatThrownBy(() -> service.resolve(null,
                new TutorContextRequest("writing", null, null, UUID.randomUUID(), "task-1", null)))
                .isInstanceOf(TutorContextException.class)
                .hasMessageContaining("Đăng nhập");

        TutorLearningContext context = service.resolve(principal(owner),
                new TutorContextRequest("writing", null, null, null, "other-user-task", null));
        assertThat(context.available()).isFalse();
        assertThat(context.writingText()).isNull();
    }

    @Test
    void guestCatalogContextContainsNoPrivateAnswerDataAndProgressIsCapped() {
        TutorLearningContext guest = service.resolve(null,
                new TutorContextRequest("reading", "reading-foundation-01", "reading-q1", null, null, null));

        assertThat(guest.available()).isTrue();
        assertThat(guest.correctAnswer()).isNull();
        assertThat(guest.contextText()).hasSizeLessThanOrEqualTo(2_000);
    }

    private AuthPrincipal principal(UUID id) { return new AuthPrincipal(id, "user@example.test", "User", UserRole.CUSTOMER); }

    private static final class FakeLearningRepository implements LearningRepository {
        @Override public void saveAttempt(LearningAttempt attempt) {}
        @Override public List<LearningAttempt> findAttempts(UUID userId) { return List.of(); }
        @Override public void saveActivity(LearningActivity activity) {}
        @Override public List<LearningActivity> findActivities(UUID userId) { return List.of(); }
    }

    private static final class FakeWritingRepository implements WritingRepository {
        private final List<WritingAssessment> items = new ArrayList<>();
        @Override public void save(WritingAssessment assessment) {}
        @Override public List<WritingAssessment> findByUser(UUID userId) {
            return items.stream().filter(item -> item.userId().equals(userId)).toList();
        }
    }

    private static final class FakeSpeakingRepository implements SpeakingRepository {
        private final List<SpeakingAttempt> items = new ArrayList<>();
        @Override public void save(SpeakingAttempt attempt) {}
        @Override public List<SpeakingAttempt> findByUser(UUID userId) {
            return items.stream().filter(item -> item.userId().equals(userId)).toList();
        }
    }

    private static final class FakePracticeAttemptStore implements PracticeAttemptStore {
        private PracticeAttemptSnapshot latest;
        @Override public void save(UUID userId, String skill, String setId, int score, int total,
                java.util.Map<String, String> answers) {}
        @Override public java.util.Optional<PracticeAttemptSnapshot> findLatest(UUID userId, String skill, String setId) {
            return latest == null || !latest.userId().equals(userId) || !latest.setId().equals(setId)
                    ? java.util.Optional.empty() : java.util.Optional.of(latest);
        }
    }
}
