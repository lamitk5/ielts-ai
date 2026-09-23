package com.ieltsaitutor.ai.routing;

import com.ieltsaitutor.ai.provider.ProviderId;

import java.time.Duration;
import java.util.UUID;

public record AiProviderTrace(UUID requestId, ProviderId provider, Duration duration, String status,
        int fallbackCount, ProviderFailureCategory failureCategory) {
}
