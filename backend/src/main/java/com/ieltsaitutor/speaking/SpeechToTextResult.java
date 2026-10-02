package com.ieltsaitutor.speaking;

import java.util.Optional;

public record SpeechToTextResult(
        String transcript,
        TranscriptSource source,
        String providerName,
        String modelVersion,
        Double confidence,
        boolean isAvailable) {

    public static SpeechToTextResult unavailable() {
        return new SpeechToTextResult(null, TranscriptSource.UNAVAILABLE, null, null, null, false);
    }

    public static SpeechToTextResult manual(String text) {
        if (text == null || text.isBlank()) {
            return unavailable();
        }
        return new SpeechToTextResult(text.trim(), TranscriptSource.MANUAL, null, null, 1.0, true);
    }

    public static SpeechToTextResult recognized(String transcript, String providerName, String modelVersion, Double confidence) {
        if (transcript == null || transcript.isBlank()) {
            return unavailable();
        }
        return new SpeechToTextResult(
                transcript.trim(),
                TranscriptSource.SPEECH_TO_TEXT,
                providerName,
                modelVersion,
                confidence,
                true
        );
    }

    public Optional<String> text() {
        return Optional.ofNullable(transcript);
    }
}
