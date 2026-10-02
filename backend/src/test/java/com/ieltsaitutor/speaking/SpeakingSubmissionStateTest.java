package com.ieltsaitutor.speaking;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpeakingSubmissionStateTest {

    @Test
    @DisplayName("Creates SpeakingSubmission with timing and prompt version pinning")
    void createsSpeakingSubmissionWithTiming() {
        UUID id = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        Instant now = Instant.now();

        SpeakingSubmission speaking = new SpeakingSubmission(
                id,
                submissionId,
                "speaking-part-2-topic-01",
                "v1",
                60,
                120,
                "audio/key/123",
                "audio/webm",
                102400L,
                "Candidate manual transcript here",
                "MANUAL",
                SpeakingSubmissionState.SUBMITTED,
                now,
                now);

        assertEquals(id, speaking.id());
        assertEquals(submissionId, speaking.submissionId());
        assertEquals("speaking-part-2-topic-01", speaking.promptId());
        assertEquals("v1", speaking.promptVersion());
        assertEquals(60, speaking.preparationSeconds());
        assertEquals(120, speaking.responseSeconds());
        assertEquals("audio/key/123", speaking.audioStorageKey());
        assertEquals("audio/webm", speaking.audioMimeType());
        assertEquals(102400L, speaking.audioSizeBytes());
        assertEquals("MANUAL", speaking.transcriptSource());
        assertEquals(SpeakingSubmissionState.SUBMITTED, speaking.status());
    }

    @Test
    @DisplayName("Speaking without audio or transcript falls back safely to text-only or UNAVAILABLE")
    void textOnlyFallbackSafe() {
        UUID id = UUID.randomUUID();
        UUID subId = UUID.randomUUID();
        Instant now = Instant.now();

        SpeakingSubmission textOnly = new SpeakingSubmission(
                id,
                subId,
                "speaking-part-1-topic-02",
                "v1",
                0,
                45,
                null,
                null,
                null,
                "Self typed transcript without audio recording",
                "MANUAL",
                SpeakingSubmissionState.SUBMITTED,
                now,
                now);

        assertNull(textOnly.audioStorageKey());
        assertEquals("MANUAL", textOnly.transcriptSource());
        assertEquals(SpeakingSubmissionState.SUBMITTED, textOnly.status());
    }

    @Test
    @DisplayName("Negative timing values are rejected")
    void rejectsNegativeTiming() {
        UUID id = UUID.randomUUID();
        UUID subId = UUID.randomUUID();
        Instant now = Instant.now();

        assertThrows(IllegalArgumentException.class, () -> new SpeakingSubmission(
                id, subId, "p1", "v1", -1, 30, null, null, null, null, "UNAVAILABLE", SpeakingSubmissionState.IN_PROGRESS, now, now));
        assertThrows(IllegalArgumentException.class, () -> new SpeakingSubmission(
                id, subId, "p1", "v1", 30, -5, null, null, null, null, "UNAVAILABLE", SpeakingSubmissionState.IN_PROGRESS, now, now));
    }
}
