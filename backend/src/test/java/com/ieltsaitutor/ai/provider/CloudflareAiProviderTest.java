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

class CloudflareAiProviderTest {
    private HttpServer server;
    private AtomicReference<String> body;
    private AtomicReference<String> authorization;
    private AtomicReference<String> path;
    private CloudflareAiProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        body = new AtomicReference<>();
        authorization = new AtomicReference<>();
        path = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        AiProviderProperties properties = new AiProviderProperties();
        properties.getCloudflare().setAccountId("account-test");
        properties.getCloudflare().setApiToken("cf-test-token");
        properties.getCloudflare().setChatModel("@cf/test-model");
        properties.getCloudflare().setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/client/v4/accounts");
        properties.getCloudflare().setResponseTimeout(Duration.ofSeconds(2));
        provider = new CloudflareAiProvider(WebClient.builder().build(), properties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() { server.stop(0); }

    @Test
    void sendsAccountModelAndNormalizesResult() {
        server.createContext("/client/v4/accounts/account-test/ai/run/@cf/test-model", exchange -> respond(exchange, 200,
                "{\"success\":true,\"result\":{\"response\":\"Hello from Cloudflare\"}}"));
        server.start();

        assertThat(provider.chat(command()).answer()).isEqualTo("Hello from Cloudflare");
        assertThat(path.get()).isEqualTo("/client/v4/accounts/account-test/ai/run/@cf/test-model");
        assertThat(authorization.get()).isEqualTo("Bearer cf-test-token");
        assertThat(body.get()).contains("Explain this");
    }

    @Test
    void mapsTemporaryFailureWithoutExposingProviderBody() {
        server.createContext("/client/v4/accounts/account-test/ai/run/@cf/test-model", exchange -> respond(exchange, 503,
                "secret provider body"));
        server.start();

        assertThatThrownBy(() -> provider.chat(command()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("AI_TEMPORARILY_UNAVAILABLE");
                    assertThat(exception.status().value()).isEqualTo(503);
                    assertThat(exception.getMessage()).doesNotContain("secret provider body");
                });
    }

    @Test
    void rejectsMalformedResponseSafely() {
        server.createContext("/client/v4/accounts/account-test/ai/run/@cf/test-model", exchange -> respond(exchange, 200, "{}"));
        server.start();

        assertThatThrownBy(() -> provider.chat(command()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> assertThat(exception.status().value()).isEqualTo(502));
    }

    @Test
    void chatCapabilityRequiresChatModelAndConnectionCredentials() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.getCloudflare().setAccountId("account");
        properties.getCloudflare().setApiToken("token");
        properties.getCloudflare().setEmbeddingModel("@cf/embedding");
        var disabledForChat = new CloudflareAiProvider(WebClient.builder().build(), properties, new ObjectMapper());

        assertThat(disabledForChat.enabled()).isFalse();
        assertThat(disabledForChat.capabilities()).doesNotContain(ProviderCapability.CHAT);
    }

    private AiChatCommand command() {
        return new AiChatCommand("Explain this", new AiChatContext("READING", null, null, null, null, null, null), List.of());
    }

    private void respond(HttpExchange exchange, int status, String response) throws IOException {
        body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        path.set(exchange.getRequestURI().getPath());
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }
}
