package com.ieltsaitutor.rag.embedding;

import com.ieltsaitutor.ai.config.AiProviderProperties;
import com.ieltsaitutor.rag.config.RagProperties;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CloudflareEmbeddingProviderTest {
    private HttpServer server;
    private AtomicReference<String> body;
    private CloudflareEmbeddingProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        body = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        AiProviderProperties properties = new AiProviderProperties();
        properties.getCloudflare().setAccountId("account-test");
        properties.getCloudflare().setApiToken("token-test");
        properties.getCloudflare().setEmbeddingModel("@cf/test-embedding");
        properties.getCloudflare().setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/client/v4/accounts");
        RagProperties rag = new RagProperties();
        provider = new CloudflareEmbeddingProvider(WebClient.builder().build(), properties, rag, new ObjectMapper());
    }

    @AfterEach
    void tearDown() { server.stop(0); }

    @Test
    void parsesExactly768DimensionsAndSendsEmbeddingTask() {
        server.createContext("/client/v4/accounts/account-test/ai/run/@cf/test-embedding", exchange ->
                respond(exchange, 200, "{\"success\":true,\"result\":{\"data\":[" + vector(768) + "]}}"));
        server.start();

        EmbeddingResult result = provider.embed(new EmbeddingRequest("hello", EmbeddingTask.QUERY));

        assertThat(result.dimension()).isEqualTo(768);
        assertThat(result.values()).hasSize(768);
        assertThat(body.get()).contains("hello");
    }

    @Test
    void rejectsDimensionMismatchWithoutPaddingOrTruncation() {
        server.createContext("/client/v4/accounts/account-test/ai/run/@cf/test-embedding", exchange ->
                respond(exchange, 200, "{\"result\":{\"data\":[" + vector(767) + "]}}"));
        server.start();

        assertThatThrownBy(() -> provider.embed(new EmbeddingRequest("hello", EmbeddingTask.DOCUMENT)))
                .isInstanceOfSatisfying(RagEmbeddingException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("RAG_EMBEDDING_DIMENSION_MISMATCH");
                    assertThat(exception.getStatus()).isEqualTo(502);
                });
    }

    private String vector(int size) {
        return IntStream.range(0, size).mapToObj(index -> "0.001").collect(java.util.stream.Collectors.joining(","));
    }

    private void respond(HttpExchange exchange, int status, String response) throws IOException {
        body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }
}
