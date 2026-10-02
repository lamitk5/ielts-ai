package com.ieltsaitutor.speaking;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public interface SpeakingAudioStorage {
    SpeakingAudioReference store(UUID userId, UUID submissionId, String mimeType, InputStream content, long sizeBytes) throws IOException;
    InputStream read(String storageKey) throws IOException;
    void delete(String storageKey);
}
