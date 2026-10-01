package com.ieltsaitutor.speaking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class SpeakingServiceTest {
    @Test
    void promptCatalogCoversAllThreeParts() {
        SpeakingService service = new SpeakingService(new UnavailableSpeechToTextProvider(), new FakeSpeakingRepository());

        assertEquals(3, service.prompts().size());
        assertEquals("PART_1", service.prompts().get(0).part());
        assertEquals("PART_2", service.prompts().get(1).part());
        assertEquals("PART_3", service.prompts().get(2).part());
    }

    @Test
    void textInputIsSavedWithoutInventingTranscriptOrBand() {
        UUID userId = UUID.randomUUID();
        SpeakingAttempt result = new SpeakingService(new UnavailableSpeechToTextProvider(), new FakeSpeakingRepository())
                .record(userId, "speaking-p1-01", "I enjoy reading in the evening.", null);

        assertEquals("INPUT_SAVED", result.status());
        assertEquals("I enjoy reading in the evening.", result.transcript());
        assertNull(result.overallBandEstimate());
    }

    @Test
    void unavailableAudioReturnsExplicitSafeState() {
        SpeakingAttempt result = new SpeakingService(new UnavailableSpeechToTextProvider(), new FakeSpeakingRepository())
                .record(UUID.randomUUID(), "speaking-p2-01", null, "audio.webm");

        assertEquals("STT_NOT_CONFIGURED", result.status());
        assertNull(result.transcript());
        assertNull(result.overallBandEstimate());
    }

    static final class FakeSpeakingRepository implements SpeakingRepository {
        @Override public void save(SpeakingAttempt attempt) {}
    }
}
