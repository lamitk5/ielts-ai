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
    private AtomicReference<String> requestPath;
    private AtomicReference<String> requestContentType;
    private AtomicInteger requestCount;
    private GeminiProperties properties;
    private GeminiAiProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        requestBody = new AtomicReference<>();
        requestKey = new AtomicReference<>();
        requestPath = new AtomicReference<>();
        requestContentType = new AtomicReference<>();
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
                "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":["
                        + "{\"thought\":true,\"text\":\"internal reasoning\"},"
                        + "{\"text\":\"Để đánh giá \"},"
                        + "{\"text\":\"website hiện tại, \"},"
                        + "{\"text\":\"mình sẽ xem xét các chức năng chính.\"}]},"
                        + "\"usageMetadata\":{\"candidatesTokenCount\":12}}]}"));
        server.start();

        var result = provider.chat(new AiChatCommand(
                "Explain this",
                new AiChatContext("WRITING", "lesson-1", null, null, "TASK_2", null, "Selected text"),
                List.of(new ChatHistoryItem("USER", "Earlier question"),
                        new ChatHistoryItem("ASSISTANT", "Earlier answer"))));

        assertThat(result.answer()).isEqualTo(
                "Để đánh giá website hiện tại, mình sẽ xem xét các chức năng chính.");
        assertThat(requestPath.get()).isEqualTo("/v1beta/models/gemini-test:generateContent");
        assertThat(requestKey.get()).isEqualTo("server-only-test-key");
        assertThat(requestContentType.get()).startsWith("application/json");
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
    void allocatesEnoughOutputBudgetForPracticeGenerationJson() {
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> respond(exchange, 200,
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{}\"}]}}]}"));
        server.start();

        provider.chat(new AiChatCommand("Generate a practice set", new AiChatContext(
                "READING", null, null, null, "practice-generation", null, null), List.of()));

        assertThat(requestBody.get()).contains("\"maxOutputTokens\":6000");
    }

    @Test
    void allowsAtMostTwoTransientRetries() {
        properties.setMaxRetries(2);
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> {
            requestCount.incrementAndGet();
            respond(exchange, 503, "temporary");
        });
        server.start();

        assertThatThrownBy(() -> provider.chat(command()))
                .isInstanceOf(AiProviderException.class);
        assertThat(requestCount).hasValue(3);
    }

    @Test
    void recoversWhenATransientFailureClearsOnRetry() {
        properties.setMaxRetries(2);
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> {
            if (requestCount.incrementAndGet() == 1) {
                respond(exchange, 503, "temporary");
                return;
            }
            respond(exchange, 200, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"LUMEN\"}]}}]}");
        });
        server.start();

        assertThat(provider.chat(command()).answer()).isEqualTo("LUMEN");
        assertThat(requestCount).hasValue(2);
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
    void mapsMalformedAndAuthErrorsWithoutExposingProviderDetails() {
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> respond(exchange, 400,
                "{\"error\":{\"message\":\"invalid request\"}}"));
        server.start();

        assertThatThrownBy(() -> provider.chat(command()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("AI_INVALID_REQUEST");
                    assertThat(exception.status().value()).isEqualTo(400);
                    assertThat(exception.getMessage()).isEqualTo("AI provider rejected the request.");
                });
    }

    @Test
    void mapsAuthenticationErrorsAsProviderConfigurationFailures() {
        properties.setMaxRetries(2);
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> {
            requestCount.incrementAndGet();
            respond(exchange, 403, "{\"error\":{\"message\":\"invalid credentials\"}}");
        });
        server.start();

        assertThatThrownBy(() -> provider.chat(command()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("AI_PROVIDER_AUTHENTICATION");
                    assertThat(exception.status().value()).isEqualTo(502);
                    assertThat(exception.getMessage()).isEqualTo(
                            "AI provider authentication is not configured correctly.");
                });
        assertThat(requestCount).hasValue(1);
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
        properties.setMaxRetries(2);
        properties.setResponseTimeout(Duration.ofMillis(40));
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> {
            requestCount.incrementAndGet();
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
        assertThat(requestCount.get()).isBetween(2, 3);
    }

    @Test
    void rejectsMalformedProviderResponseSafely() {
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> respond(exchange, 200, "{\"candidates\":[]}"));
        server.start();

        assertThatThrownBy(() -> provider.chat(command()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("AI_PROVIDER_MALFORMED_RESPONSE");
                    assertThat(exception.status().value()).isEqualTo(502);
                });
    }

    private AiChatCommand command() {
        return new AiChatCommand("Hello", new AiChatContext("GENERAL", null, null, null, null, null, null), List.of());
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        requestKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
        requestPath.set(exchange.getRequestURI().getPath());
        requestContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }
}
