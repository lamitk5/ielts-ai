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
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

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
    void sendsTrustedGroundedEvidenceForDocumentContext() {
        server.createContext("/openai/v1/chat/completions", exchange -> respond(exchange, 200,
                "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"LUMEN-TXT-92841\"}}]}"));
        server.start();

        provider.chat(new AiChatCommand("Which code?", new AiChatContext("GENERAL", null, null, null, null, null, null),
                List.of(), "request-1", "[notes.txt, chunk 0]\\nSECRET_CODE=LUMEN-TXT-92841", List.of(),
                Set.of(ProviderCapability.DOCUMENT_CONTEXT)));

        assertThat(body.get()).contains("LUMEN-TXT-92841");
    }

    @Test
    void allocatesEnoughOutputBudgetForPracticeGenerationJson() {
        server.createContext("/openai/v1/chat/completions", exchange -> respond(exchange, 200,
                "{\"choices\":[{\"message\":{\"content\":\"{}\"}}]}"));
        server.start();

        provider.chat(new AiChatCommand("Generate a practice set", new AiChatContext(
                "READING", null, null, null, "practice-generation", null, null), List.of()));

        assertThat(body.get()).contains("\"max_completion_tokens\":6000");
    }

    @Test
    void requestsJsonObjectForWritingAssessment() {
        server.createContext("/openai/v1/chat/completions", exchange -> respond(exchange, 200,
                "{\"choices\":[{\"message\":{\"content\":\"{}\"}}]}"));
        server.start();

        provider.chat(new AiChatCommand(
                "Assess this IELTS writing response. Return JSON only with overallBandEstimate, criteria, strengths, issues, suggestions.",
                new AiChatContext("WRITING", null, "task-1-academic-01", null, "TASK_1", null, "A writing response."),
                List.of()));

        assertThat(body.get()).contains("\"response_format\":{\"type\":\"json_object\"}");
    }

    @Test
    void advertisesTextDocumentContextCapability() {
        assertThat(provider.capabilities()).contains(ProviderCapability.CHAT, ProviderCapability.DOCUMENT_CONTEXT);
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
    void retriesRateLimitAfterProviderDelayAndStopsAfterBoundedAttempts() {
        AtomicInteger attempts = new AtomicInteger();
        AtomicReference<Duration> delay = new AtomicReference<>();
        server.createContext("/openai/v1/chat/completions", exchange -> {
            if (attempts.getAndIncrement() == 0) {
                exchange.getResponseHeaders().set("Retry-After", "1");
                respond(exchange, 429, "rate limited");
            } else {
                respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"recovered\"}}]}");
            }
        });
        server.start();
        provider = new GroqAiProvider(WebClient.builder().build(), configuredProperties(), new ObjectMapper(), delay::set);

        assertThat(provider.chat(command()).answer()).isEqualTo("recovered");
        assertThat(attempts).hasValue(2);
        assertThat(delay.get()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void doesNotImmediatelyResendWhenTokenBudgetIsExhausted() {
        AtomicInteger attempts = new AtomicInteger();
        server.createContext("/openai/v1/chat/completions", exchange -> {
            attempts.incrementAndGet();
            exchange.getResponseHeaders().set("Retry-After", "1");
            exchange.getResponseHeaders().set("x-ratelimit-remaining-tokens", "0");
            respond(exchange, 429, "token budget exhausted");
        });
        server.start();

        assertThatThrownBy(() -> provider.chat(command()))
                .isInstanceOf(AiProviderException.class)
                .satisfies(exception -> assertThat(((AiProviderException) exception).code()).isEqualTo("AI_RATE_LIMITED"));
        assertThat(attempts).hasValue(1);
    }

    @Test
    void prefersShortestProviderResetWindowWhenRetryAfterIsAbsent() {
        AtomicReference<Duration> delay = new AtomicReference<>();
        server.createContext("/openai/v1/chat/completions", exchange -> {
            exchange.getResponseHeaders().set("x-ratelimit-reset-requests", "1m26.4s");
            exchange.getResponseHeaders().set("x-ratelimit-reset-tokens", "1.095s");
            respond(exchange, 429, "rate limited");
        });
        server.start();
        provider = new GroqAiProvider(WebClient.builder().build(), configuredProperties(), new ObjectMapper(), delay::set);

        assertThatThrownBy(() -> provider.chat(command())).isInstanceOf(AiProviderException.class);
        assertThat(delay.get()).isEqualTo(Duration.ofMillis(1_095));
    }

    @Test
    void boundsAndDeduplicatesHistoryBeforeProviderCall() {
        server.createContext("/openai/v1/chat/completions", exchange -> respond(exchange, 200,
                "{\"choices\":[{\"message\":{\"content\":\"bounded\"}}]}"));
        server.start();
        List<com.ieltsaitutor.ai.dto.ChatHistoryItem> history = List.of(
                new com.ieltsaitutor.ai.dto.ChatHistoryItem("USER", "duplicate"),
                new com.ieltsaitutor.ai.dto.ChatHistoryItem("USER", "duplicate"),
                new com.ieltsaitutor.ai.dto.ChatHistoryItem("ASSISTANT", "x".repeat(5_000)),
                new com.ieltsaitutor.ai.dto.ChatHistoryItem("USER", "latest"));

        provider.chat(new AiChatCommand("Explain", null, history));

        assertThat(body.get()).contains("latest");
        assertThat(body.get()).doesNotContain("x".repeat(5_000));
        assertThat(countOccurrences(body.get(), "\"content\":\"duplicate\"")).isEqualTo(1);
    }

    private AiProviderProperties configuredProperties() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.getGroq().setApiKey("groq-test-key");
        properties.getGroq().setChatModel("groq-test-model");
        properties.getGroq().setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/openai/v1");
        properties.getGroq().setResponseTimeout(Duration.ofMillis(300));
        return properties;
    }

    @Test
    void rejectsMalformedResponseSafely() {
        server.createContext("/openai/v1/chat/completions", exchange -> respond(exchange, 200, "{\"choices\":[]}"));
        server.start();

        assertThatThrownBy(() -> provider.chat(command()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("AI_PROVIDER_MALFORMED_RESPONSE");
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

    private int countOccurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
