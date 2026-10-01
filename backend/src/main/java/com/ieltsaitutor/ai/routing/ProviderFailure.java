package com.ieltsaitutor.ai.routing;

import com.ieltsaitutor.ai.provider.ProviderId;

public record ProviderFailure(ProviderId provider, ProviderFailureCategory category, String code) {
}
