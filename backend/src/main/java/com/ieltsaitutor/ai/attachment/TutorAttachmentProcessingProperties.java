package com.ieltsaitutor.ai.attachment;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TutorAttachmentProcessingProperties {
    private final Duration staleTimeout;
    private final int maxRecoveryAttempts;

    public TutorAttachmentProcessingProperties(
            @Value("${ai.tutor.attachment.processing.stale-timeout:5m}") Duration staleTimeout,
            @Value("${ai.tutor.attachment.processing.max-recovery-attempts:1}") int maxRecoveryAttempts) {
        this.staleTimeout = staleTimeout;
        this.maxRecoveryAttempts = maxRecoveryAttempts;
    }

    public Duration staleTimeout() { return staleTimeout; }
    public int maxRecoveryAttempts() { return maxRecoveryAttempts; }
}
