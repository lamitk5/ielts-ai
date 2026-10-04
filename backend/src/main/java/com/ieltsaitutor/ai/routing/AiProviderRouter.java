package com.ieltsaitutor.ai.routing;

import com.ieltsaitutor.ai.config.AiProviderProperties;
import com.ieltsaitutor.ai.exception.AiProviderException;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.ai.provider.ProviderCapability;
import com.ieltsaitutor.ai.provider.ProviderId;
import com.ieltsaitutor.admin.portal.ApiUsageService;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Primary
public class AiProviderRouter implements AiProvider {
    private static final Logger log = LoggerFactory.getLogger(AiProviderRouter.class);
    private final Map<ProviderId, AiProviderAdapter> adapters;
    private final List<ProviderId> configuredOrder;
    private final ProviderRoutingPolicy policy;
    private final ProviderHealthRegistry health;
    private final ApiUsageService usage;

    public AiProviderRouter(List<AiProviderAdapter> adapters) {
        this(adapters, null, new ProviderHealthRegistry(new AiProviderProperties.Health(), java.time.Clock.systemUTC()), null);
    }

    @Autowired
    public AiProviderRouter(List<AiProviderAdapter> adapters, AiProviderProperties properties,
            ProviderHealthRegistry health, ApiUsageService usage) {
        this.adapters = adapters.stream().collect(Collectors.toUnmodifiableMap(AiProviderAdapter::id, Function.identity()));
        this.configuredOrder = properties == null
                ? adapters.stream().map(AiProviderAdapter::id).toList()
                : orderedProviders(properties);
        this.policy = new ProviderRoutingPolicy();
        this.health = health;
        this.usage = usage;
    }

    @Override
    public AiChatResult chat(AiChatCommand command) {
        AiProviderException lastTransient = null;
        java.util.Set<ProviderCapability> requiredCapabilities = requiredCapabilities(command);
        int attempt = 0;
        for (ProviderId providerId : configuredOrder) {
            AiProviderAdapter adapter = adapters.get(providerId);
            if (adapter == null || !adapter.enabled() || !adapter.capabilities().containsAll(requiredCapabilities)) {
                continue;
            }
            if (!health.tryAcquire(providerId)) continue;
            try {
                long started = System.nanoTime();
                AiChatResult result = adapter.chat(command);
                health.recordSuccess(providerId);
                if (usage != null) {
                    try {
                        usage.record(providerId.name(), null, "TEXT_CHAT", "SUCCESS",
                                (System.nanoTime() - started) / 1_000_000, attempt > 0);
                    } catch (RuntimeException telemetryFailure) {
                        log.warn("AI usage telemetry skipped provider={} reason={}", providerId,
                                telemetryFailure.getClass().getSimpleName());
                    }
                }
                return result;
            } catch (AiProviderException exception) {
                ProviderFailure failure = policy.classify(providerId, exception);
                if (!policy.shouldAdvance(failure)) throw exception;
                if (failure.category() == ProviderFailureCategory.TRANSIENT) {
                    health.recordFailure(providerId);
                }
                lastTransient = exception;
            }
            attempt++;
        }
        throw new AiProviderException("AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                "Trợ giảng AI tạm thời chưa sẵn sàng.", lastTransient);
    }

    private java.util.Set<ProviderCapability> requiredCapabilities(AiChatCommand command) {
        EnumSet<ProviderCapability> required = EnumSet.of(ProviderCapability.CHAT);
        if (command != null && command.requiredCapabilities() != null) {
            required.addAll(command.requiredCapabilities());
        }
        return required;
    }

    private List<ProviderId> orderedProviders(AiProviderProperties properties) {
        List<ProviderId> order = new ArrayList<>();
        if (properties.getPrimaryProvider() != null) order.add(properties.getPrimaryProvider());
        order.addAll(properties.getFallbackProviders());
        return List.copyOf(order);
    }
}
