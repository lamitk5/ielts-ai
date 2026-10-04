package com.ieltsaitutor.ai.routing;

import com.ieltsaitutor.ai.provider.ProviderId;

public record ProviderAttempt(ProviderId provider, boolean attempted) {
}
