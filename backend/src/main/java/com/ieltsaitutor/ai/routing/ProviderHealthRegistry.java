package com.ieltsaitutor.ai.routing;

import com.ieltsaitutor.ai.config.AiProviderProperties;
import com.ieltsaitutor.ai.provider.ProviderId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Map;

@Component
public class ProviderHealthRegistry {
    private final AiProviderProperties.Health configuration;
    private final Map<ProviderId, State> states = new EnumMap<>(ProviderId.class);
    private Clock clock;

    @Autowired
    public ProviderHealthRegistry(AiProviderProperties properties) {
        this(properties.getHealth(), Clock.systemUTC());
    }

    public ProviderHealthRegistry(AiProviderProperties.Health configuration, Clock clock) {
        this.configuration = configuration;
        this.clock = clock;
    }

    public synchronized boolean tryAcquire(ProviderId provider) {
        State state = states.computeIfAbsent(provider, ignored -> new State());
        Instant now = clock.instant();
        prune(state, now);
        if (state.failures.size() < configuration.getTransientFailureThreshold()) return true;
        Instant lastFailure = state.failures.peekLast();
        if (lastFailure.plus(configuration.getCooldown()).isAfter(now)) return false;
        if (state.probeInFlight) return false;
        state.probeInFlight = true;
        return true;
    }

    public synchronized void recordFailure(ProviderId provider) {
        State state = states.computeIfAbsent(provider, ignored -> new State());
        state.failures.addLast(clock.instant());
        state.probeInFlight = false;
        prune(state, clock.instant());
    }

    public synchronized void recordSuccess(ProviderId provider) {
        State state = states.computeIfAbsent(provider, ignored -> new State());
        state.failures.clear();
        state.probeInFlight = false;
    }

    public synchronized ProviderHealthSnapshot snapshot(ProviderId provider) {
        State state = states.computeIfAbsent(provider, ignored -> new State());
        prune(state, clock.instant());
        boolean coolingDown = state.failures.size() >= configuration.getTransientFailureThreshold()
                && state.failures.peekLast() != null
                && state.failures.peekLast().plus(configuration.getCooldown()).isAfter(clock.instant());
        return new ProviderHealthSnapshot(provider, state.failures.size(), coolingDown, state.probeInFlight);
    }

    public synchronized void setClock(Clock clock) { this.clock = clock; }

    private void prune(State state, Instant now) {
        Duration window = configuration.getRollingWindow();
        while (!state.failures.isEmpty() && state.failures.peekFirst().plus(window).isBefore(now)) {
            state.failures.removeFirst();
        }
    }

    private static final class State {
        private final ArrayDeque<Instant> failures = new ArrayDeque<>();
        private boolean probeInFlight;
    }
}
