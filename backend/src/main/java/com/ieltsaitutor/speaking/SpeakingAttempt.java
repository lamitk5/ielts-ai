package com.ieltsaitutor.speaking;

import java.time.Instant;
import java.util.UUID;

public record SpeakingAttempt(UUID id, UUID userId, String promptId, String transcript, String audioFilename,
        String status, Double overallBandEstimate, Instant createdAt) {}
