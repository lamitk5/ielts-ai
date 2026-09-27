package com.ieltsaitutor.speaking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.ieltsaitutor.learning.intelligence.LearningEvidencePipeline;

@Service
public class SpeakingService {
    private final SpeechToTextProvider speechToText;
    private final SpeakingRepository repository;
    private final LearningEvidencePipeline evidence;

    public SpeakingService(SpeechToTextProvider speechToText, SpeakingRepository repository) {
        this(speechToText, repository, null);
    }

    @Autowired
    public SpeakingService(SpeechToTextProvider speechToText, SpeakingRepository repository, LearningEvidencePipeline evidence) {
        this.speechToText = speechToText; this.repository = repository; this.evidence = evidence;
    }

    public List<SpeakingPrompt> prompts() { return List.of(
            new SpeakingPrompt("speaking-p1-01", "PART_1", "Do you enjoy reading in your free time?"),
            new SpeakingPrompt("speaking-p2-01", "PART_2", "Describe a place where you like to study."),
            new SpeakingPrompt("speaking-p3-01", "PART_3", "How can cities support lifelong learning?")); }

    public SpeakingAttempt record(UUID userId, String promptId, String transcript, String audioFilename) {
        validatePrompt(promptId);
        SpeakingAttempt attempt = buildResult(UUID.randomUUID(), userId, promptId, transcript, audioFilename, Instant.now());
        repository.save(attempt);
        publish(userId, promptId, attempt);
        return attempt;
    }

    public SpeakingAttempt startAttempt(UUID userId, String promptId) {
        validatePrompt(promptId);
        return repository.start(userId, promptId);
    }

    public SpeakingAttempt saveAttemptDraft(UUID userId, UUID attemptId, String transcript) {
        SpeakingAttempt attempt = owned(userId, attemptId);
        if (!"IN_PROGRESS".equals(attempt.status()) && !"DRAFT".equals(attempt.status())) {
            throw new IllegalStateException("Speaking attempt đã được nộp");
        }
        String normalized = normalize(transcript);
        return repository.saveDraft(new SpeakingAttempt(attempt.id(), attempt.userId(), attempt.promptId(), normalized,
                attempt.audioFilename(), normalized == null ? "IN_PROGRESS" : "DRAFT", null, attempt.createdAt()));
    }

    public SpeakingAttempt getAttempt(UUID userId, UUID attemptId) { return owned(userId, attemptId); }

    public SpeakingAttempt submitAttempt(UUID userId, UUID attemptId, String transcript) {
        SpeakingAttempt attempt = owned(userId, attemptId);
        if (!"IN_PROGRESS".equals(attempt.status()) && !"DRAFT".equals(attempt.status())) return attempt;
        SpeakingAttempt result = buildResult(attempt.id(), userId, attempt.promptId(),
                transcript == null ? attempt.transcript() : transcript, attempt.audioFilename(), attempt.createdAt());
        SpeakingAttempt completed = repository.complete(attempt, result);
        publish(userId, attempt.promptId(), completed);
        return completed;
    }

    private SpeakingAttempt buildResult(UUID id, UUID userId, String promptId, String transcript, String audioFilename, Instant createdAt) {
        String resolvedTranscript = normalize(transcript);
        String status = resolvedTranscript != null ? "INPUT_SAVED" : "STT_NOT_CONFIGURED";
        if (resolvedTranscript == null && audioFilename != null && !audioFilename.isBlank()) {
            resolvedTranscript = speechToText.transcribe(audioFilename).map(String::trim).filter(value -> !value.isBlank()).orElse(null);
            if (resolvedTranscript != null) status = "RECORDED";
        }
        return new SpeakingAttempt(id, userId, promptId, resolvedTranscript, audioFilename, status, null, createdAt);
    }

    private void publish(UUID userId, String promptId, SpeakingAttempt attempt) {
        if (evidence != null) {
            try { evidence.speakingSubmitted(userId, promptId, attempt.transcript() != null, attempt.createdAt()); }
            catch (RuntimeException ignored) { /* adaptive refresh must not break speaking persistence */ }
        }
    }

    public List<SpeakingAttempt> attempts(UUID userId) { return repository.findByUser(userId); }

    private void validatePrompt(String promptId) {
        if (prompts().stream().noneMatch(prompt -> prompt.id().equals(promptId))) throw new IllegalArgumentException("Speaking prompt not found");
    }

    private SpeakingAttempt owned(UUID userId, UUID attemptId) {
        return repository.findByUserAndId(userId, attemptId).filter(attempt -> attempt.userId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Speaking attempt không tồn tại"));
    }

    private String normalize(String transcript) { return transcript == null || transcript.isBlank() ? null : transcript.trim(); }
}
