package com.ieltsaitutor.ai.routing;

import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.ProviderCapability;
import com.ieltsaitutor.ai.provider.ProviderId;

import java.util.Set;

public interface AiProviderAdapter {
    ProviderId id();

    Set<ProviderCapability> capabilities();

    boolean enabled();

    AiChatResult chat(AiChatCommand command);
}
