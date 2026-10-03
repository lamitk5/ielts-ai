package com.ieltsaitutor.ai.routing;

import com.ieltsaitutor.ai.exception.AiProviderException;
import com.ieltsaitutor.ai.config.AiProviderProperties;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.ProviderCapability;
import com.ieltsaitutor.ai.provider.ProviderId;
import com.ieltsaitutor.admin.portal.ApiUsageService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import org.springframework.http.HttpStatus;

class AiProviderFallbackTest {
    private final AiChatCommand command = new AiChatCommand("hello", null, List.of(), "request-1");

    @Test
    void rotates429FromGroqToCloudflareAndStopsAfterSuccess() {
        AiProviderAdapter groq = failing(ProviderId.GROQ, new AiProviderException("AI_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS, "busy"));
        AiProviderAdapter cloudflare = successful(ProviderId.CLOUDFLARE, "cloudflare");
        AiProviderAdapter gemini = successful(ProviderId.GEMINI, "gemini");
        AiProviderRouter router = new AiProviderRouter(List.of(groq, cloudflare, gemini));
        clearInvocations(groq, cloudflare, gemini);

        assertThat(router.chat(command).answer()).isEqualTo("cloudflare");
        verify(groq).chat(command);
        verify(cloudflare).chat(command);
        verifyNoInteractions(gemini);
    }

    @Test
    void rotatesTimeoutFromGroqToCloudflare() {
        AiProviderAdapter groq = failing(ProviderId.GROQ, new AiProviderException("AI_TIMEOUT", HttpStatus.GATEWAY_TIMEOUT, "timeout"));
        AiProviderAdapter cloudflare = successful(ProviderId.CLOUDFLARE, "cloudflare");

        assertThat(new AiProviderRouter(List.of(groq, cloudflare)).chat(command).answer()).isEqualTo("cloudflare");
    }

    @Test
    void rotatesCloudflareTemporaryFailureToGemini() {
        AiProviderAdapter cloudflare = failing(ProviderId.CLOUDFLARE,
                new AiProviderException("AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "down"));
        AiProviderAdapter gemini = successful(ProviderId.GEMINI, "gemini");

        assertThat(new AiProviderRouter(List.of(cloudflare, gemini)).chat(command).answer()).isEqualTo("gemini");
    }

    @Test
    void invalidRequestDoesNotRotate() {
        AiProviderAdapter groq = failing(ProviderId.GROQ,
                new AiProviderException("AI_INVALID_REQUEST", HttpStatus.BAD_REQUEST, "invalid"));
        AiProviderAdapter cloudflare = successful(ProviderId.CLOUDFLARE, "cloudflare");
        AiProviderRouter router = new AiProviderRouter(List.of(groq, cloudflare));
        clearInvocations(groq, cloudflare);

        assertThatThrownBy(() -> router.chat(command))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("invalid");
        verifyNoInteractions(cloudflare);
    }

    @Test
    void allTransientProvidersReturnControlledUnavailable() {
        AiProviderAdapter groq = failing(ProviderId.GROQ,
                new AiProviderException("AI_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS, "busy"));
        AiProviderAdapter cloudflare = failing(ProviderId.CLOUDFLARE,
                new AiProviderException("AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "down"));
        AiProviderAdapter gemini = failing(ProviderId.GEMINI,
                new AiProviderException("AI_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS, "busy"));

        assertThatThrownBy(() -> new AiProviderRouter(List.of(groq, cloudflare, gemini)).chat(command))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("AI_TEMPORARILY_UNAVAILABLE");
                    assertThat(exception.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
    }

    @Test
    void telemetryFailureDoesNotTurnSuccessfulProviderResponseIntoProviderFailure() {
        AiProviderAdapter groq = successful(ProviderId.GROQ, "groq");
        AiProviderProperties properties = new AiProviderProperties();
        ApiUsageService usage = mock(ApiUsageService.class);
        doThrow(new DataAccessResourceFailureException("telemetry table unavailable"))
                .when(usage).record(anyString(), isNull(), anyString(), anyString(), anyLong(), anyBoolean());

        AiProviderRouter router = new AiProviderRouter(
                List.of(groq),
                properties,
                new ProviderHealthRegistry(properties.getHealth(), java.time.Clock.systemUTC()),
                usage);

        assertThat(router.chat(command).answer()).isEqualTo("groq");
    }

    private AiProviderAdapter successful(ProviderId id, String answer) {
        AiProviderAdapter adapter = mock(AiProviderAdapter.class);
        when(adapter.id()).thenReturn(id);
        when(adapter.enabled()).thenReturn(true);
        when(adapter.capabilities()).thenReturn(Set.of(ProviderCapability.CHAT));
        when(adapter.chat(command)).thenReturn(AiChatResult.answered(answer));
        return adapter;
    }

    private AiProviderAdapter failing(ProviderId id, AiProviderException exception) {
        AiProviderAdapter adapter = mock(AiProviderAdapter.class);
        when(adapter.id()).thenReturn(id);
        when(adapter.enabled()).thenReturn(true);
        when(adapter.capabilities()).thenReturn(Set.of(ProviderCapability.CHAT));
        when(adapter.chat(command)).thenThrow(exception);
        return adapter;
    }
}
