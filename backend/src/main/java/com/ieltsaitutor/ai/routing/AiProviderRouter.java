package com.ieltsaitutor.ai.routing;

import com.ieltsaitutor.ai.config.AiProviderProperties;
import com.ieltsaitutor.ai.exception.AiProviderException;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.ai.provider.ProviderCapability;
import com.ieltsaitutor.ai.provider.ProviderId;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    private final Map<ProviderId, AiProviderAdapter> adapters;
    private final List<ProviderId> configuredOrder;

    public AiProviderRouter(List<AiProviderAdapter> adapters) {
        this(adapters, null);
    }

    @Autowired
    public AiProviderRouter(List<AiProviderAdapter> adapters, AiProviderProperties properties) {
        this.adapters = adapters.stream().collect(Collectors.toUnmodifiableMap(AiProviderAdapter::id, Function.identity()));
        this.configuredOrder = properties == null
                ? adapters.stream().map(AiProviderAdapter::id).toList()
                : orderedProviders(properties);
    }

    @Override
    public AiChatResult chat(AiChatCommand command) {
        for (ProviderId providerId : configuredOrder) {
            AiProviderAdapter adapter = adapters.get(providerId);
            if (adapter == null || !adapter.enabled() || !adapter.capabilities().contains(ProviderCapability.CHAT)) {
                continue;
            }
            return adapter.chat(command);
        }
        throw new AiProviderException("AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                "Trợ giảng AI tạm thời chưa sẵn sàng.");
    }

    private List<ProviderId> orderedProviders(AiProviderProperties properties) {
        List<ProviderId> order = new ArrayList<>();
        if (properties.getPrimaryProvider() != null) order.add(properties.getPrimaryProvider());
        order.addAll(properties.getFallbackProviders());
        return List.copyOf(order);
    }
}
