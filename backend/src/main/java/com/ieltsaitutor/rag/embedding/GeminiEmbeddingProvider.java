package com.ieltsaitutor.rag.embedding;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import com.ieltsaitutor.rag.config.RagProperties;

@Component
public class GeminiEmbeddingProvider implements EmbeddingProvider {
    private final WebClient webClient;
    private final GeminiEmbeddingProperties properties;
    private final ObjectMapper objectMapper;
    private final RagProperties ragProperties;

    @Autowired
    public GeminiEmbeddingProvider(WebClient geminiWebClient, GeminiEmbeddingProperties properties,
            ObjectMapper objectMapper, RagProperties ragProperties) {
        this.webClient = geminiWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.ragProperties = ragProperties;
    }

    public GeminiEmbeddingProvider(WebClient geminiWebClient, GeminiEmbeddingProperties properties,
            ObjectMapper objectMapper) {
        this(geminiWebClient, properties, objectMapper, new RagProperties());
    }

    public GeminiEmbeddingProvider(WebClient geminiWebClient, GeminiEmbeddingProperties properties) {
        this(geminiWebClient, properties, new ObjectMapper());
    }

    @Override
    public com.ieltsaitutor.ai.provider.ProviderId providerId() {
        return com.ieltsaitutor.ai.provider.ProviderId.GEMINI;
    }

    @Override
    public boolean isEmbeddingConfigured() {
        return !properties.getApiKey().isBlank() && !properties.getEmbeddingModel().isBlank()
                && !properties.getBaseUrl().isBlank() && properties.getEmbeddingDimension() == 768;
    }

    @Override
    public EmbeddingSpace embeddingSpace() {
        return new EmbeddingSpace(providerId(), properties.getEmbeddingModel(), properties.getEmbeddingDimension(),
                ragProperties.embeddingVersion());
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        validateConfiguration();
        for (int attempt = 0; ; attempt++) {
            try {
                String body = webClient.post()
                        .uri(endpointUri())
                        .header("x-goog-api-key", properties.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(buildRequest(request))
                        .exchangeToMono(response -> response.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(value -> response.statusCode().isError()
                                        ? reactor.core.publisher.Mono.error(new UpstreamException(response.statusCode().value()))
                                        : reactor.core.publisher.Mono.just(value)))
                        .block(properties.getResponseTimeout());
                return parseResponse(body);
            } catch (UpstreamException exception) {
                if (isRetryable(exception.status) && attempt < properties.getMaxRetries()) {
                    pause(attempt);
                    continue;
                }
                if (exception.status == 429) {
                    throw new RagEmbeddingException("RAG_EMBEDDING_RATE_LIMITED", 429,
                            "Embedding provider đang giới hạn tốc độ.");
                }
                throw new RagEmbeddingException("RAG_EMBEDDING_UNAVAILABLE", 503,
                        "Embedding provider tạm thời chưa sẵn sàng.");
            } catch (JacksonException | IllegalArgumentException exception) {
                if (exception instanceof RagEmbeddingException ragException) throw ragException;
                throw new RagEmbeddingException("RAG_EMBEDDING_INVALID", 502,
                        "Embedding provider trả về dữ liệu không hợp lệ.", exception);
            } catch (WebClientRequestException | IllegalStateException exception) {
                throw new RagEmbeddingException("RAG_EMBEDDING_UNAVAILABLE", 503,
                        "Embedding provider tạm thời chưa sẵn sàng.", exception);
            }
        }
    }

    @Override
    public List<EmbeddingResult> embedBatch(List<EmbeddingRequest> requests) {
        if (requests == null) throw new IllegalArgumentException("Embedding requests are required");
        List<EmbeddingResult> results = new ArrayList<>();
        for (EmbeddingRequest request : requests) results.add(embed(request));
        return List.copyOf(results);
    }

    private void validateConfiguration() {
        if (properties.getApiKey().isBlank()) {
            throw new RagEmbeddingException("RAG_EMBEDDING_UNAVAILABLE", 503,
                    "Embedding provider chưa được cấu hình.");
        }
        if (properties.getEmbeddingModel().isBlank()) {
            throw new RagEmbeddingException("RAG_EMBEDDING_NOT_CONFIGURED", 500,
                    "Embedding model chưa được cấu hình.");
        }
        if (properties.getEmbeddingDimension() <= 0) {
            throw new RagEmbeddingException("RAG_EMBEDDING_NOT_CONFIGURED", 500,
                    "Embedding dimension không hợp lệ.");
        }
    }

    private String buildRequest(EmbeddingRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", "models/" + properties.getEmbeddingModel());
        root.put("taskType", request.task() == EmbeddingTask.QUERY ? "RETRIEVAL_QUERY" : "RETRIEVAL_DOCUMENT");
        root.put("output_dimensionality", properties.getEmbeddingDimension());
        root.putObject("content").putArray("parts").addObject().put("text", request.text());
        return root.toString();
    }

    private EmbeddingResult parseResponse(String body) throws JacksonException {
        JsonNode values = objectMapper.readTree(body).path("embedding").path("values");
        if (!values.isArray() || values.isEmpty()) {
            throw new IllegalArgumentException("missing embedding values");
        }
        List<Float> vector = new ArrayList<>();
        for (JsonNode value : values) {
            if (!value.isNumber()) throw new IllegalArgumentException("non-numeric embedding value");
            vector.add(value.floatValue());
        }
        if (vector.size() != properties.getEmbeddingDimension()) {
            throw new RagEmbeddingException("RAG_EMBEDDING_DIMENSION_MISMATCH", 502,
                    "Embedding dimension không khớp cấu hình.");
        }
        EmbeddingSpace space = vector.size() == 768 ? embeddingSpace() : null;
        return new EmbeddingResult(properties.getEmbeddingModel(), vector.size(), vector, space);
    }

    private String endpointUri() {
        return properties.getBaseUrl().replaceAll("/$", "") + "/" + properties.getEmbeddingModel() + ":embedContent";
    }

    private boolean isRetryable(int status) { return status == 429 || status == 503 || status >= 500; }

    private void pause(int attempt) {
        try { Thread.sleep(50L * (1L << attempt)); }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RagEmbeddingException("RAG_EMBEDDING_UNAVAILABLE", 503,
                    "Embedding provider tạm thời chưa sẵn sàng.", exception);
        }
    }

    private static final class UpstreamException extends RuntimeException {
        private final int status;
        private UpstreamException(int status) { this.status = status; }
    }
}
