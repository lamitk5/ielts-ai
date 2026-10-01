package com.ieltsaitutor.tutor.security;

import com.ieltsaitutor.ai.routing.ProviderFailure;
import com.ieltsaitutor.ai.routing.ProviderFailureCategory;
import com.ieltsaitutor.ai.routing.AiProviderTrace;
import com.ieltsaitutor.ai.provider.ProviderId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiSecurityRegressionTest {
    @Test
    void providerFailureAndTraceContainOnlySafeBoundedFields() {
        ProviderFailure failure = new ProviderFailure(ProviderId.GROQ, ProviderFailureCategory.TRANSIENT, "AI_TEMPORARILY_UNAVAILABLE");
        AiProviderTrace trace = new AiProviderTrace(UUID.randomUUID(), ProviderId.GROQ, Duration.ofMillis(12),
                "FAILED", 1, ProviderFailureCategory.TRANSIENT);

        assertThat(failure.toString()).doesNotContain("Authorization", "apiKey", "essay", "prompt");
        assertThat(trace.toString()).doesNotContain("Authorization", "apiKey", "essay", "prompt");
    }
}
