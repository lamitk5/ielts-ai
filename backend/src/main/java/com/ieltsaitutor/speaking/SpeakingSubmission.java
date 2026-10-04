package com.ieltsaitutor.speaking;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SpeakingSubmission(
        UUID id,
        UUID submissionId,
        String promptId,
        String promptVersion,
        int preparationSeconds,
        int responseSeconds,
        String audioStorageKey,
        String audioMimeType,
        Long audioSizeBytes,
        String transcript,
        String transcriptSource,
        SpeakingSubmissionState status,
        Instant createdAt,
        Instant updatedAt) {

    public SpeakingSubmission {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(submissionId, "submissionId must not be null");
        Objects.requireNonNull(promptId, "promptId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        promptVersion = promptVersion != null && !promptVersion.isBlank() ? promptVersion.trim() : "v1";
        transcriptSource = transcriptSource != null && !transcriptSource.isBlank() ? transcriptSource.trim() : "UNAVAILABLE";
        if (preparationSeconds < 0) throw new IllegalArgumentException("preparationSeconds cannot be negative");
        if (responseSeconds < 0) throw new IllegalArgumentException("responseSeconds cannot be negative");
        if (audioSizeBytes != null && audioSizeBytes <= 0) throw new IllegalArgumentException("audioSizeBytes must be positive");
    }

    public SpeakingSubmission withAudio(String key, String mimeType, long sizeBytes) {
        return new SpeakingSubmission(
                id, submissionId, promptId, promptVersion, preparationSeconds, responseSeconds,
                key, mimeType, sizeBytes, transcript, transcriptSource, status, createdAt, Instant.now());
    }

    public SpeakingSubmission withTranscript(String text, String source) {
        return new SpeakingSubmission(
                id, submissionId, promptId, promptVersion, preparationSeconds, responseSeconds,
                audioStorageKey, audioMimeType, audioSizeBytes, text, source, status, createdAt, Instant.now());
    }

    public SpeakingSubmission withStatus(SpeakingSubmissionState newStatus) {
        return new SpeakingSubmission(
                id, submissionId, promptId, promptVersion, preparationSeconds, responseSeconds,
                audioStorageKey, audioMimeType, audioSizeBytes, transcript, transcriptSource, newStatus, createdAt, Instant.now());
    }
}
