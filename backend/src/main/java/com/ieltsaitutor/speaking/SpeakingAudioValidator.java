package com.ieltsaitutor.speaking;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class SpeakingAudioValidator {

    public static final long MAX_AUDIO_SIZE_BYTES = 25 * 1024 * 1024; // 25 MB

    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "audio/webm",
            "audio/ogg",
            "audio/mp4",
            "audio/wav",
            "audio/x-wav",
            "audio/mpeg",
            "audio/mp3",
            "video/webm" // Some browsers record audio into webm container labeled video/webm
    );

    public void validate(String mimeType, String filename, long sizeBytes, byte[] sample) {
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("Audio file is empty");
        }
        if (sizeBytes > MAX_AUDIO_SIZE_BYTES) {
            throw new IllegalArgumentException("Audio file exceeds 25MB maximum limit");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("Audio MIME type is required");
        }
        String normalizedMime = mimeType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_MIME_TYPES.contains(normalizedMime)) {
            throw new IllegalArgumentException("Unsupported audio format: " + mimeType);
        }
    }
}
