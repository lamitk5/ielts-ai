package com.ieltsaitutor.rag.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.sun.net.httpserver.HttpServer;

class GeminiEmbeddingProviderTest {
    private HttpServer server;
    private AtomicReference<String> requestBody;

    @BeforeEach
    void setUp() throws IOException {
        requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendsConfiguredEmbeddingModel() {
        respond(200, "{\"embedding\":{\"values\":[0.1,0.2]}}");
        GeminiEmbeddingProvider provider = provider("configured-model", 2);

        provider.embed(new EmbeddingRequest("hello", EmbeddingTask.DOCUMENT));

        assertThat(requestBody.get()).contains("configured-model");
    }

    @Test
    void embedsDocumentText() {
        respond(200, "{\"embedding\":{\"values\":[0.1,0.2]}}");
        EmbeddingResult result = provider("model", 2)
                .embed(new EmbeddingRequest("document text", EmbeddingTask.DOCUMENT));

        assertThat(result.values()).containsExactly(0.1f, 0.2f);
        assertThat(requestBody.get()).contains("RETRIEVAL_DOCUMENT", "document text");
    }

    @Test
    void requestsConfiguredOutputDimensionality() throws Exception {
        respond(200, embeddingResponse(768));

        provider("model", 768).embed(new EmbeddingRequest("text", EmbeddingTask.DOCUMENT));

        JsonNode request = new ObjectMapper().readTree(requestBody.get());
        assertThat(request.get("output_dimensionality")).isNotNull();
        assertThat(request.get("output_dimensionality").intValue()).isEqualTo(768);
    }

    @Test
    void requestsConfiguredOutputDimensionalityForAlternateConfiguration() throws Exception {
        respond(200, embeddingResponse(1536));

        provider("model", 1536).embed(new EmbeddingRequest("text", EmbeddingTask.DOCUMENT));

        JsonNode request = new ObjectMapper().readTree(requestBody.get());
        assertThat(request.get("output_dimensionality")).isNotNull();
        assertThat(request.get("output_dimensionality").intValue()).isEqualTo(1536);
    }

    @Test
    void embedsQueryText() {
        respond(200, "{\"embedding\":{\"values\":[0.3,0.4]}}");
        EmbeddingVector result = new DefaultQueryEmbeddingService(provider("model", 2))
                .embedQuery("query text");

        assertThat(result.values()).containsExactly(0.3f, 0.4f);
        assertThat(requestBody.get()).contains("RETRIEVAL_QUERY", "query text");
    }

    @Test
    void mapsMalformedResponse() {
        respond(200, "{\"unexpected\":true}");

        assertThatThrownBy(() -> provider("model", 2)
                .embed(new EmbeddingRequest("text", EmbeddingTask.DOCUMENT)))
                .isInstanceOf(RagEmbeddingException.class)
                .extracting("code").isEqualTo("RAG_EMBEDDING_INVALID");
    }

    @Test
    void mapsMissingApiKey() {
        GeminiEmbeddingProperties properties = properties("model", 2);

        assertThatThrownBy(() -> new GeminiEmbeddingProvider(WebClient.create(), properties)
                .embed(new EmbeddingRequest("text", EmbeddingTask.DOCUMENT)))
                .isInstanceOf(RagEmbeddingException.class)
                .extracting("code").isEqualTo("RAG_EMBEDDING_UNAVAILABLE");
    }

    @Test
    void mapsProvider429() {
        respond(429, "rate limited");

        assertThatThrownBy(() -> provider("model", 2)
                .embed(new EmbeddingRequest("text", EmbeddingTask.DOCUMENT)))
                .isInstanceOf(RagEmbeddingException.class)
                .extracting("code").isEqualTo("RAG_EMBEDDING_RATE_LIMITED");
    }

    @Test
    void mapsProvider503() {
        respond(503, "unavailable");

        assertThatThrownBy(() -> provider("model", 2)
                .embed(new EmbeddingRequest("text", EmbeddingTask.DOCUMENT)))
                .isInstanceOf(RagEmbeddingException.class)
                .extracting("code").isEqualTo("RAG_EMBEDDING_UNAVAILABLE");
    }

    @Test
    void rejectsDimensionMismatch() {
        respond(200, "{\"embedding\":{\"values\":[0.1]}}");

        assertThatThrownBy(() -> provider("model", 2)
                .embed(new EmbeddingRequest("text", EmbeddingTask.DOCUMENT)))
                .isInstanceOf(RagEmbeddingException.class)
                .extracting("code").isEqualTo("RAG_EMBEDDING_DIMENSION_MISMATCH");
    }

    private GeminiEmbeddingProvider provider(String model, int dimension) {
        return new GeminiEmbeddingProvider(WebClient.create(), properties(model, dimension));
    }

    private GeminiEmbeddingProperties properties(String model, int dimension) {
        GeminiEmbeddingProperties properties = new GeminiEmbeddingProperties();
        properties.setApiKey("test-key");
        properties.setEmbeddingModel(model);
        properties.setEmbeddingDimension(dimension);
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/v1beta/models");
        properties.setMaxRetries(1);
        return properties;
    }

    private void respond(int status, String body) {
        server.createContext("/", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
    }

    private String embeddingResponse(int dimension) {
        return "{\"embedding\":{\"values\":["
                + String.join(",", Collections.nCopies(dimension, "0.1"))
                + "]}}";
    }
}
