package com.ieltsaitutor.ai.routing;

import com.ieltsaitutor.ai.config.AiProviderProperties;
import com.ieltsaitutor.ai.provider.ProviderId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderHealthRegistryTest {
    @Test
    void opensAfterThreeFailuresAndAllowsOneProbeAfterCooldown() {
        AiProviderProperties.Health health = new AiProviderProperties.Health();
        health.setTransientFailureThreshold(3);
        health.setCooldown(Duration.ofSeconds(30));
        var now = new java.util.concurrent.atomic.AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        var clock = Clock.fixed(now.get(), ZoneOffset.UTC);
        ProviderHealthRegistry registry = new ProviderHealthRegistry(health, clock);

        registry.recordFailure(ProviderId.GROQ);
        registry.recordFailure(ProviderId.GROQ);
        registry.recordFailure(ProviderId.GROQ);
        assertThat(registry.tryAcquire(ProviderId.GROQ)).isFalse();

        now.set(Instant.parse("2026-01-01T00:00:31Z"));
        clock = Clock.fixed(now.get(), ZoneOffset.UTC);
        registry.setClock(clock);
        assertThat(registry.tryAcquire(ProviderId.GROQ)).isTrue();
        assertThat(registry.tryAcquire(ProviderId.GROQ)).isFalse();
        registry.recordSuccess(ProviderId.GROQ);
        assertThat(registry.tryAcquire(ProviderId.GROQ)).isTrue();
    }
}
