package com.ieltsaitutor.ai.provider;

import java.util.Set;

public record ProviderConfiguration(
        ProviderId id,
        boolean enabled,
        String model,
        Set<ProviderCapability> capabilities,
        String displayId
) {
    public ProviderConfiguration {
        capabilities = Set.copyOf(capabilities);
    }
}
