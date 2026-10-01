package com.ieltsaitutor.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LearningProgressServiceTest {
    @Test
    void emptyMemberReceivesAllFourSkillsWithNoPersonalBand() {
        UUID userId = UUID.randomUUID();
        LearningProgressService service = new LearningProgressService(new FakeLearningRepository());

        MemberProgress progress = service.progress(userId);

        assertEquals(List.of("Reading", "Listening", "Writing", "Speaking"),
                progress.skills().stream().map(SkillProgress::skill).toList());
        assertEquals(4, progress.skills().stream().filter(skill -> skill.band() == null).count());
    }

    @Test
    void aggregatesOnlyTheRequestedMembersAttemptsAndRoundsToHalfBands() {
        UUID userId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        FakeLearningRepository repository = new FakeLearningRepository();
        repository.attempts.add(new LearningAttempt(UUID.randomUUID(), userId, "reading", 17, 20, Instant.now()));
        repository.attempts.add(new LearningAttempt(UUID.randomUUID(), otherUser, "reading", 2, 20, Instant.now()));

        MemberProgress progress = new LearningProgressService(repository).progress(userId);

        assertEquals(7.5, progress.skills().get(0).band());
        assertEquals(0, progress.skills().get(1).band() == null ? 0 : 1);
    }

    @Test
    void cannotReadAnotherMembersAttempts() {
        UUID userId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        FakeLearningRepository repository = new FakeLearningRepository();
        repository.attempts.add(new LearningAttempt(UUID.randomUUID(), otherUser, "reading", 19, 20, Instant.now()));

        assertEquals(0, new LearningProgressService(repository).attempts(userId).size());
        assertThrows(IllegalArgumentException.class,
                () -> new LearningProgressService(repository).recordAttempt(userId,
                        new LearningAttempt(UUID.randomUUID(), otherUser, "reading", 1, 1, Instant.now())));
    }

    static final class FakeLearningRepository implements LearningRepository {
        final List<LearningAttempt> attempts = new ArrayList<>();
        final List<LearningActivity> activities = new ArrayList<>();

        @Override public void saveAttempt(LearningAttempt attempt) { attempts.add(attempt); }
        @Override public List<LearningAttempt> findAttempts(UUID userId) { return attempts.stream().filter(a -> a.userId().equals(userId)).toList(); }
        @Override public void saveActivity(LearningActivity activity) { activities.add(activity); }
        @Override public List<LearningActivity> findActivities(UUID userId) { return activities.stream().filter(a -> a.userId().equals(userId)).toList(); }
    }
}
