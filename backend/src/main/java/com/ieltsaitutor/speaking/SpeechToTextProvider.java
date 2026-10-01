package com.ieltsaitutor.speaking;

import java.util.Optional;

public interface SpeechToTextProvider {
    Optional<String> transcribe(String audioFilename);
}
