package com.ieltsaitutor.speaking;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class UnavailableSpeechToTextProvider implements SpeechToTextProvider {
    @Override public Optional<String> transcribe(String audioFilename) { return Optional.empty(); }
}
