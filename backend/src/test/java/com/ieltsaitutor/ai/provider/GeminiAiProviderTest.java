package com.ieltsaitutor.ai.provider;

import com.ieltsaitutor.ai.config.GeminiProperties;
import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.ChatHistoryItem;
import com.ieltsaitutor.ai.exception.AiProviderException;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiAiProviderTest {
    private HttpServer server;
    private AtomicReference<String> requestBody;
    private AtomicReference<String> requestKey;
    private AtomicInteger requestCount;
    private GeminiProperties properties;
    private GeminiAiProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        requestBody = new AtomicReference<>();
        requestKey = new AtomicReference<>();
        requestCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        properties = new GeminiProperties();
        properties.setApiKey("server-only-test-key");
        properties.setModel("gemini-test");
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/v1beta/models");
        properties.setResponseTimeout(Duration.ofMillis(300));
        properties.setMaxRetries(0);
        provider = new GeminiAiProvider(WebClient.builder().build(), properties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() { server.stop(0); }

    @Test
    void sendsServerKeyMapsHistoryAndParsesAnswer() {
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> respond(exchange, 200,
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Gemini answer\"}]}}]}"));
        server.start();

        var result = provider.chat(new AiChatCommand(
                "Explain this",
                new AiChatContext("WRITING", "lesson-1", null, null, "TASK_2", null, "Selected text"),
                List.of(new ChatHistoryItem("USER", "Earlier question"),
                        new ChatHistoryItem("ASSISTANT", "Earlier answer"))));

        assertThat(result.answer()).isEqualTo("Gemini answer");
        assertThat(requestKey.get()).isEqualTo("server-only-test-key");
        assertThat(requestBody.get()).contains("You are an AI learning assistant");
        assertThat(requestBody.get()).contains("\"role\":\"model\"");
        assertThat(requestBody.get()).contains("Earlier question");
        assertThat(requestBody.get()).contains("Selected text");
        assertThat(requestBody.get()).contains("\"thinkingConfig\"");
        assertThat(requestBody.get()).contains("\"thinkingLevel\":\"low\"");
    }

    @Test
    void defaultsToGemini38FlashWithoutOverridingEnvironmentConfiguration() {
        assertThat(new GeminiProperties().getModel()).isEqualTo("gemini-3.8-flash");
    }

    @Test
    void mapsRateLimitSafely() {
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> respond(exchange, 429, "secret provider body"));
        server.start();

        assertThatThrownBy(() -> provider.chat(command()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("AI_RATE_LIMITED");
                    assertThat(exception.status().value()).isEqualTo(429);
                });
    }

    @Test
    void mapsUnavailableSafely() {
        properties.setMaxRetries(1);
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> {
            requestCount.incrementAndGet();
            respond(exchange, 503, "secret provider body");
        });
        server.start();

        assertThatThrownBy(() -> provider.chat(command()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("AI_TEMPORARILY_UNAVAILABLE");
                    assertThat(exception.status().value()).isEqualTo(503);
                });
        assertThat(requestCount).hasValue(2);
    }

    @Test
    void mapsTimeoutWithoutExposingProviderDetails() {
        properties.setResponseTimeout(Duration.ofMillis(40));
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> {
            try {
                Thread.sleep(200);
                respond(exchange, 200, "{}");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });
        server.start();

        assertThatThrownBy(() -> provider.chat(command()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("AI_TIMEOUT");
                    assertThat(exception.status().value()).isEqualTo(504);
                });
    }

    @Test
    void rejectsMalformedProviderResponseSafely() {
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> respond(exchange, 200, "{\"candidates\":[]}"));
        server.start();

        assertThatThrownBy(() -> provider.chat(command()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("AI_PROVIDER_ERROR");
                    assertThat(exception.status().value()).isEqualTo(502);
                });
    }

    private AiChatCommand command() {
        return new AiChatCommand("Hello", new AiChatContext("GENERAL", null, null, null, null, null, null), List.of());
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        requestKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }
}
