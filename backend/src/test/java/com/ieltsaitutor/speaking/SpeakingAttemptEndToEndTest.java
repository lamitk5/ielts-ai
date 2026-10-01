package com.ieltsaitutor.speaking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SpeakingAttemptEndToEndTest {
    @Test
    void manualTranscriptUsesOnePersistentAttemptAndNeverInventsBand() {
        UUID userId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        SpeakingRepository repository = mock(SpeakingRepository.class);
        SpeakingAttempt initial = new SpeakingAttempt(attemptId, userId, "speaking-p1-01", null, null,
                "IN_PROGRESS", null, Instant.now());
        when(repository.start(userId, "speaking-p1-01")).thenReturn(initial);
        when(repository.findByUserAndId(userId, attemptId)).thenReturn(Optional.of(initial));
        when(repository.saveDraft(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.complete(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        SpeakingService service = new SpeakingService(new UnavailableSpeechToTextProvider(), repository);
        SpeakingAttempt started = service.startAttempt(userId, "speaking-p1-01");
        SpeakingAttempt saved = service.saveAttemptDraft(userId, attemptId, "I enjoy reading in the evening.");
        SpeakingAttempt completed = service.submitAttempt(userId, attemptId, saved.transcript());

        assertEquals(attemptId, started.id());
        assertEquals("I enjoy reading in the evening.", saved.transcript());
        assertEquals("INPUT_SAVED", completed.status());
        assertEquals(saved.transcript(), completed.transcript());
        assertNull(completed.overallBandEstimate());
        verify(repository).saveDraft(any());
        verify(repository).complete(any(), eq(completed));
    }
}
