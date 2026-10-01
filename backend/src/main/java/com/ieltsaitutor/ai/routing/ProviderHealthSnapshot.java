package com.ieltsaitutor.ai.routing;

import com.ieltsaitutor.ai.provider.ProviderId;

public record ProviderHealthSnapshot(ProviderId provider, int transientFailures, boolean coolingDown,
        boolean halfOpenProbeInFlight) {
}
