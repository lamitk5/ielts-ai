package com.ieltsaitutor.speaking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class SpeakingService {
    private final SpeechToTextProvider speechToText;
    private final SpeakingRepository repository;

    public SpeakingService(SpeechToTextProvider speechToText, SpeakingRepository repository) {
        this.speechToText = speechToText; this.repository = repository;
    }

    public List<SpeakingPrompt> prompts() { return List.of(
            new SpeakingPrompt("speaking-p1-01", "PART_1", "Do you enjoy reading in your free time?"),
            new SpeakingPrompt("speaking-p2-01", "PART_2", "Describe a place where you like to study."),
            new SpeakingPrompt("speaking-p3-01", "PART_3", "How can cities support lifelong learning?")); }

    public SpeakingAttempt record(UUID userId, String promptId, String transcript, String audioFilename) {
        if (prompts().stream().noneMatch(prompt -> prompt.id().equals(promptId))) throw new IllegalArgumentException("Speaking prompt not found");
        String resolvedTranscript = transcript == null || transcript.isBlank() ? null : transcript.trim();
        String status = resolvedTranscript != null ? "INPUT_SAVED" : "STT_NOT_CONFIGURED";
        if (resolvedTranscript == null && audioFilename != null && !audioFilename.isBlank()) {
            resolvedTranscript = speechToText.transcribe(audioFilename).filter(value -> !value.isBlank()).orElse(null);
            if (resolvedTranscript != null) status = "RECORDED";
        }
        SpeakingAttempt attempt = new SpeakingAttempt(UUID.randomUUID(), userId, promptId, resolvedTranscript,
                audioFilename, status, null, Instant.now());
        repository.save(attempt);
        return attempt;
    }

    public List<SpeakingAttempt> attempts(UUID userId) { return repository.findByUser(userId); }
}
