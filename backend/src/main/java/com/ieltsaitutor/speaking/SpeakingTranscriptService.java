package com.ieltsaitutor.speaking;

import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SpeakingTranscriptService {
    private final SpeechToTextProvider sttProvider;

    public SpeakingTranscriptService(SpeechToTextProvider sttProvider) {
        this.sttProvider = Objects.requireNonNull(sttProvider, "sttProvider must not be null");
    }

    public SpeechToTextResult resolveTranscript(String manualTranscript, String audioStorageKey) {
        if (manualTranscript != null && !manualTranscript.isBlank()) {
            return SpeechToTextResult.manual(manualTranscript);
        }
        if (audioStorageKey != null && !audioStorageKey.isBlank()) {
            return sttProvider.process(audioStorageKey);
        }
        return SpeechToTextResult.unavailable();
    }
}
