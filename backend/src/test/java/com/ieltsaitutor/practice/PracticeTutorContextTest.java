package com.ieltsaitutor.practice;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PracticeTutorContextTest {
    @Test
    void readingAndListeningSubmissionExposesOnlyStableAttemptReference() {
        PracticeAttemptStore store = mock(PracticeAttemptStore.class);
        UUID readingAttempt = UUID.randomUUID();
        UUID listeningAttempt = UUID.randomUUID();
        when(store.saveAndReturn(any(), eq("reading"), eq("reading-foundation-01"), eq(1), eq(2), any()))
                .thenReturn(readingAttempt);
        when(store.saveAndReturn(any(), eq("listening"), eq("listening-foundation-01"), eq(1), eq(2), any()))
                .thenReturn(listeningAttempt);
        PracticeService service = new PracticeService(SyntheticPracticeCatalog.inMemory(), store);

        PracticeAttemptResult reading = service.submit("reading", "reading-foundation-01",
                Map.of("reading-q1", "B"), UUID.randomUUID());
        PracticeAttemptResult listening = service.submit("listening", "listening-foundation-01",
                Map.of("listening-q1", "B"), UUID.randomUUID());

        assertThat(reading.attemptId()).isEqualTo(readingAttempt);
        assertThat(listening.attemptId()).isEqualTo(listeningAttempt);
        assertThat(reading.answers()).containsOnlyKeys("reading-q1");
        assertThat(listening.answers()).containsOnlyKeys("listening-q1");
        assertThat(reading.review().get(0).correctAnswer()).isEqualTo("B");
    }
}
