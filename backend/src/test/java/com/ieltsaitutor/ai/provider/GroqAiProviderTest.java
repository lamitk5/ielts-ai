package com.ieltsaitutor.ai.provider;

import com.ieltsaitutor.ai.config.AiProviderProperties;
import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.exception.AiProviderException;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroqAiProviderTest {
    private HttpServer server;
    private AtomicReference<String> body;
    private AtomicReference<String> authorization;
    private GroqAiProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        body = new AtomicReference<>();
        authorization = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        AiProviderProperties properties = new AiProviderProperties();
        properties.getGroq().setApiKey("groq-test-key");
        properties.getGroq().setChatModel("groq-test-model");
        properties.getGroq().setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/openai/v1");
        properties.getGroq().setResponseTimeout(Duration.ofMillis(300));
        provider = new GroqAiProvider(WebClient.builder().build(), properties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() { server.stop(0); }

    @Test
    void sendsConfiguredModelAndServerKeyAndNormalizesAssistantText() {
        server.createContext("/openai/v1/chat/completions", exchange -> respond(exchange, 200,
                "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hello from Groq\"}}]}"));
        server.start();

        assertThat(provider.chat(command()).answer()).isEqualTo("Hello from Groq");
        assertThat(authorization.get()).isEqualTo("Bearer groq-test-key");
        assertThat(body.get()).contains("\"model\":\"groq-test-model\"");
        assertThat(body.get()).contains("Explain this");
    }

    @Test
    void mapsRateLimitWithoutExposingProviderBody() {
        server.createContext("/openai/v1/chat/completions", exchange -> respond(exchange, 429, "secret provider body"));
        server.start();

        assertThatThrownBy(() -> provider.chat(command()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("AI_RATE_LIMITED");
                    assertThat(exception.status().value()).isEqualTo(429);
                    assertThat(exception.getMessage()).doesNotContain("secret provider body");
                });
    }

    @Test
    void rejectsMalformedResponseSafely() {
        server.createContext("/openai/v1/chat/completions", exchange -> respond(exchange, 200, "{\"choices\":[]}"));
        server.start();

        assertThatThrownBy(() -> provider.chat(command()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("AI_PROVIDER_ERROR");
                    assertThat(exception.status().value()).isEqualTo(502);
                });
    }

    @Test
    void missingConfigurationDisablesProvider() {
        AiProviderProperties properties = new AiProviderProperties();
        var disabled = new GroqAiProvider(WebClient.builder().build(), properties, new ObjectMapper());

        assertThat(disabled.enabled()).isFalse();
        assertThat(disabled.capabilities()).isEmpty();
        assertThatThrownBy(() -> disabled.chat(command())).isInstanceOf(AiProviderException.class);
    }

    private AiChatCommand command() {
        return new AiChatCommand("Explain this", new AiChatContext("READING", null, null, null, null, null, null), List.of());
    }

    private void respond(HttpExchange exchange, int status, String response) throws IOException {
        body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }
}
