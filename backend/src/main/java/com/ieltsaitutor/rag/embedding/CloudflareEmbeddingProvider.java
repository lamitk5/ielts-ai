package com.ieltsaitutor.rag.embedding;

import com.ieltsaitutor.ai.config.AiProviderProperties;
import com.ieltsaitutor.rag.config.RagProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Component
public class CloudflareEmbeddingProvider implements EmbeddingProvider {
    private final WebClient webClient;
    private final AiProviderProperties.Cloudflare properties;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;

    public CloudflareEmbeddingProvider(@Qualifier("cloudflareWebClient") WebClient webClient,
            AiProviderProperties properties, RagProperties ragProperties, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.properties = properties.getCloudflare();
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public com.ieltsaitutor.ai.provider.ProviderId providerId() {
        return com.ieltsaitutor.ai.provider.ProviderId.CLOUDFLARE;
    }

    @Override
    public boolean isEmbeddingConfigured() {
        return !properties.getAccountId().isBlank() && !properties.getApiToken().isBlank()
                && !properties.getEmbeddingModel().isBlank() && !properties.getBaseUrl().isBlank();
    }

    @Override
    public EmbeddingSpace embeddingSpace() {
        return new EmbeddingSpace(providerId(), properties.getEmbeddingModel(), ragProperties.embeddingDimension(),
                "v1");
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        validateConfiguration();
        try {
            String body = webClient.post()
                    .uri(endpoint())
                    .header("Authorization", "Bearer " + properties.getApiToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.createObjectNode().putArray("text").add(request.text()).toString())
                    .exchangeToMono(response -> response.bodyToMono(String.class).defaultIfEmpty("")
                            .flatMap(value -> response.statusCode().isError()
                                    ? reactor.core.publisher.Mono.error(new UpstreamException(response.statusCode().value()))
                                    : reactor.core.publisher.Mono.just(value)))
                    .block(properties.getResponseTimeout());
            return parseResponse(body);
        } catch (UpstreamException exception) {
            int status = exception.status;
            if (status == 429) throw new RagEmbeddingException("RAG_EMBEDDING_RATE_LIMITED", 429,
                    "Embedding provider đang giới hạn tốc độ.");
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

    @Override
    public List<EmbeddingResult> embedBatch(List<EmbeddingRequest> requests) {
        if (requests == null) throw new IllegalArgumentException("Embedding requests are required");
        List<EmbeddingResult> results = new ArrayList<>();
        for (EmbeddingRequest request : requests) results.add(embed(request));
        return List.copyOf(results);
    }

    private void validateConfiguration() {
        if (properties.getAccountId().isBlank() || properties.getApiToken().isBlank()
                || properties.getEmbeddingModel().isBlank() || properties.getBaseUrl().isBlank()) {
            throw new RagEmbeddingException("RAG_EMBEDDING_NOT_CONFIGURED", 500,
                    "Cloudflare embedding provider chưa được cấu hình.");
        }
        if (ragProperties.embeddingDimension() != 768) {
            throw new RagEmbeddingException("RAG_EMBEDDING_DIMENSION_MISMATCH", 502,
                    "Embedding dimension không khớp vector(768).");
        }
    }

    private EmbeddingResult parseResponse(String body) throws JacksonException {
        JsonNode data = objectMapper.readTree(body).path("result").path("data");
        JsonNode values = data.path(0).isArray() ? data.path(0) : data;
        if (!values.isArray() || values.isEmpty()) throw new IllegalArgumentException("missing embedding values");
        List<Float> vector = new ArrayList<>();
        for (JsonNode value : values) {
            if (!value.isNumber()) throw new IllegalArgumentException("non-numeric embedding value");
            vector.add(value.floatValue());
        }
        if (vector.size() != ragProperties.embeddingDimension()) {
            throw new RagEmbeddingException("RAG_EMBEDDING_DIMENSION_MISMATCH", 502,
                    "Embedding dimension không khớp cấu hình.");
        }
        EmbeddingSpace space = embeddingSpace();
        return new EmbeddingResult(properties.getEmbeddingModel(), vector.size(), vector, space);
    }

    private String endpoint() {
        return properties.getBaseUrl().replaceAll("/$", "") + "/" + properties.getAccountId()
                + "/ai/run/" + properties.getEmbeddingModel();
    }

    private static final class UpstreamException extends RuntimeException {
        private final int status;
        private UpstreamException(int status) { this.status = status; }
    }
}
