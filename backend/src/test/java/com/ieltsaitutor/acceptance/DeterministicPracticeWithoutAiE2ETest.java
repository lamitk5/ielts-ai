package com.ieltsaitutor.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.PracticeAttemptStore;
import com.ieltsaitutor.practice.PracticeService;
import com.ieltsaitutor.practice.SyntheticPracticeCatalog;
import com.ieltsaitutor.practice.attempt.AttemptService;

class DeterministicPracticeWithoutAiE2ETest {
    @Test
    void catalogAndObjectiveScoringDoNotDependOnAnAiProvider() {
        AttemptService durableAttempts = mock(AttemptService.class);
        PracticeService practice = new PracticeService(SyntheticPracticeCatalog.inMemory(), mock(PracticeAttemptStore.class), durableAttempts);

        assertEquals(2, practice.sets("reading").get(0).questions().size());
        verifyNoInteractions(durableAttempts);
        assertEquals("reading", practice.sets("reading").get(0).skill().toLowerCase());
    }
}
