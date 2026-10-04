package com.ieltsaitutor.speaking;

import java.util.Optional;

public interface SpeechToTextProvider {
    Optional<String> transcribe(String audioFilename);

    default SpeechToTextResult process(String audioKeyOrFilename) {
        return transcribe(audioKeyOrFilename)
                .filter(text -> !text.isBlank())
                .map(text -> SpeechToTextResult.recognized(text.trim(), "legacy-provider", "v1", 1.0))
                .orElseGet(SpeechToTextResult::unavailable);
    }
}
