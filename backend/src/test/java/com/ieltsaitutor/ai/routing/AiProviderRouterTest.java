package com.ieltsaitutor.ai.routing;

import com.ieltsaitutor.ai.config.AiProviderProperties;
import com.ieltsaitutor.ai.config.ProviderConfigurationRegistry;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.ProviderCapability;
import com.ieltsaitutor.ai.provider.ProviderId;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AiProviderRouterTest {
    private final AiChatCommand command = new AiChatCommand("hello", null, List.of(), "request-1");

    @Test
    void usesFirstEnabledProviderAndReturnsNormalizedResult() {
        AiProviderAdapter first = adapter(ProviderId.GROQ, true, AiChatResult.answered("groq"));
        AiProviderAdapter second = adapter(ProviderId.GEMINI, true, AiChatResult.answered("gemini"));

        AiProviderRouter router = new AiProviderRouter(List.of(first, second));
        clearInvocations(first, second);

        assertThat(router.chat(command).answer()).isEqualTo("groq");
        verify(first).chat(command);
        verifyNoInteractions(second);
    }

    @Test
    void skipsDisabledProviders() {
        AiProviderAdapter disabled = adapter(ProviderId.GROQ, false, AiChatResult.answered("unused"));
        AiProviderAdapter enabled = adapter(ProviderId.GEMINI, true, AiChatResult.answered("gemini"));

        AiProviderRouter router = new AiProviderRouter(List.of(disabled, enabled));
        clearInvocations(disabled, enabled);

        assertThat(router.chat(command).answer()).isEqualTo("gemini");
        verify(disabled, never()).chat(command);
        verify(enabled).chat(command);
    }

    @Test
    void failsWithControlledUnavailableWhenNoProviderIsEnabled() {
        AiProviderAdapter disabled = adapter(ProviderId.GROQ, false, AiChatResult.answered("unused"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new AiProviderRouter(List.of(disabled)).chat(command))
                .isInstanceOf(com.ieltsaitutor.ai.exception.AiProviderException.class)
                .hasMessage("Trợ giảng AI tạm thời chưa sẵn sàng.");
    }

    private AiProviderAdapter adapter(ProviderId id, boolean enabled, AiChatResult result) {
        AiProviderAdapter adapter = mock(AiProviderAdapter.class);
        when(adapter.id()).thenReturn(id);
        when(adapter.enabled()).thenReturn(enabled);
        when(adapter.capabilities()).thenReturn(Set.of(ProviderCapability.CHAT));
        when(adapter.chat(command)).thenReturn(result);
        return adapter;
    }
}
