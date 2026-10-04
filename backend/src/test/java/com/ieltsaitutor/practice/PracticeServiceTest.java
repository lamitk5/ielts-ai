package com.ieltsaitutor.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PracticeServiceTest {
    @Test
    void catalogLoadsSyntheticReadingAndListeningSets() {
        SyntheticPracticeCatalog catalog = SyntheticPracticeCatalog.inMemory();

        assertEquals("Reading", catalog.sets("reading").get(0).skill());
        assertEquals("Listening", catalog.sets("listening").get(0).skill());
        assertEquals(2, catalog.sets("reading").get(0).questions().size());
    }

    @Test
    void scoresOnlyAgainstImmutableAnswerKeyAndReturnsReview() {
        PracticeService service = new PracticeService(SyntheticPracticeCatalog.inMemory(), new FakePracticeAttemptStore());
        PracticeSet set = service.sets("reading").get(0);

        PracticeAttemptResult result = service.submit("reading", set.id(), Map.of("reading-q1", "B", "reading-q2", "A"), java.util.UUID.randomUUID());

        assertEquals(2, result.score());
        assertEquals(2, result.total());
        assertEquals(2, result.review().size());
    }

    @Test
    void invalidSkillOrSetCannotBeSubmitted() {
        PracticeService service = new PracticeService(SyntheticPracticeCatalog.inMemory(), new FakePracticeAttemptStore());

        assertThrows(IllegalArgumentException.class, () -> service.sets("writing"));
        assertThrows(IllegalArgumentException.class, () -> service.submit("reading", "missing", Map.of(), java.util.UUID.randomUUID()));
    }

    static final class FakePracticeAttemptStore implements PracticeAttemptStore {
        @Override public void save(UUID userId, String skill, String setId, int score, int total, Map<String, String> answers) {}
    }
}
