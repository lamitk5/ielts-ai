package com.ieltsaitutor.speaking;

import java.util.Objects;
import java.util.UUID;

public record SpeakingAudioReference(
        String storageKey,
        String mimeType,
        long sizeBytes,
        String checksumSha256) {

    public SpeakingAudioReference {
        Objects.requireNonNull(storageKey, "storageKey must not be null");
        Objects.requireNonNull(mimeType, "mimeType must not be null");
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("sizeBytes must be positive");
        }
    }
}
