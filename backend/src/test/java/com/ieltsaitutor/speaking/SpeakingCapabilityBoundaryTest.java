package com.ieltsaitutor.speaking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class SpeakingCapabilityBoundaryTest {
    @Test
    void unavailableSttKeepsAudioBoundaryExplicitAndDoesNotCreateTranscriptOrBand() {
        SpeakingRepository repository = mock(SpeakingRepository.class);
        SpeakingAttempt result = new SpeakingService(new UnavailableSpeechToTextProvider(), repository)
                .record(UUID.randomUUID(), "speaking-p2-01", null, "local-recording.webm");

        assertEquals("STT_NOT_CONFIGURED", result.status());
        assertNull(result.transcript());
        assertNull(result.overallBandEstimate());
    }
}
