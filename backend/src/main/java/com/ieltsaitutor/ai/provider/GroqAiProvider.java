package com.ieltsaitutor.ai.provider;

import com.ieltsaitutor.ai.config.AiProviderProperties;
import com.ieltsaitutor.ai.exception.AiProviderException;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.routing.AiProviderAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Set;

@Component
public class GroqAiProvider implements AiProviderAdapter {
    private final WebClient webClient;
    private final AiProviderProperties.Groq properties;
    private final ObjectMapper objectMapper;

    public GroqAiProvider(@Qualifier("groqWebClient") WebClient webClient,
            AiProviderProperties properties, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.properties = properties.getGroq();
        this.objectMapper = objectMapper;
    }

    @Override
    public ProviderId id() { return ProviderId.GROQ; }

    @Override
    public Set<ProviderCapability> capabilities() {
        return enabled() ? Set.of(ProviderCapability.CHAT) : Set.of();
    }

    @Override
    public boolean enabled() {
        return !properties.getApiKey().isBlank() && !properties.getChatModel().isBlank()
                && !properties.getBaseUrl().isBlank();
    }

    @Override
    public AiChatResult chat(AiChatCommand command) {
        if (!enabled()) throw unavailable();
        try {
            String body = webClient.post()
                    .uri(endpoint())
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(buildRequest(command))
                    .exchangeToMono(response -> response.bodyToMono(String.class).defaultIfEmpty("")
                            .flatMap(responseBody -> response.statusCode().isError()
                                    ? reactor.core.publisher.Mono.error(new GroqHttpException(response.statusCode().value(), responseBody))
                                    : reactor.core.publisher.Mono.just(responseBody)))
                    .block(properties.getResponseTimeout());
            return AiChatResult.answered(parseAnswer(body));
        } catch (GroqHttpException exception) {
            throw map(exception);
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new AiProviderException("AI_PROVIDER_MALFORMED_RESPONSE", HttpStatus.BAD_GATEWAY,
                    "AI provider returned an invalid response.", exception);
        } catch (WebClientRequestException exception) {
            throw new AiProviderException("AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    "Trợ giảng AI tạm thời chưa sẵn sàng.", exception);
        } catch (IllegalStateException exception) {
            if (exception.getMessage() != null && exception.getMessage().toLowerCase().contains("timeout")) {
                throw new AiProviderException("AI_TIMEOUT", HttpStatus.GATEWAY_TIMEOUT,
                        "Kết nối tới Trợ giảng AI đã hết thời gian.", exception);
            }
            throw new AiProviderException("AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    "Trợ giảng AI tạm thời chưa sẵn sàng.", exception);
        }
    }

    private String buildRequest(AiChatCommand command) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getChatModel());
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content", TutorSystemInstruction.TEXT);
        command.history().forEach(history -> messages.addObject()
                .put("role", "ASSISTANT".equals(history.role()) ? "assistant" : "user")
                .put("content", history.content()));
        messages.addObject().put("role", "user").put("content", command.message());
        return root.toString();
    }

    private String parseAnswer(String body) throws JacksonException {
        JsonNode content = objectMapper.readTree(body).path("choices").path(0).path("message").path("content");
        if (!content.isTextual() || content.asText().isBlank()) throw new IllegalArgumentException("missing answer text");
        return content.asText().trim();
    }

    private String endpoint() { return properties.getBaseUrl().replaceAll("/$", "") + "/chat/completions"; }

    private AiProviderException map(GroqHttpException exception) {
        if (exception.statusCode == 400) return new AiProviderException("AI_INVALID_REQUEST", HttpStatus.BAD_REQUEST,
                "AI provider rejected the request.");
        if (exception.statusCode == 401 || exception.statusCode == 403) return new AiProviderException("AI_PROVIDER_AUTHENTICATION",
                HttpStatus.BAD_GATEWAY, "AI provider authentication is not configured correctly.");
        if (exception.statusCode == 429) return new AiProviderException("AI_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS,
                "Trợ giảng AI đang nhận quá nhiều yêu cầu. Vui lòng thử lại sau.");
        return new AiProviderException("AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                "Trợ giảng AI tạm thời chưa sẵn sàng.");
    }

    private AiProviderException unavailable() {
        return new AiProviderException("AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                "Trợ giảng AI tạm thời chưa sẵn sàng.");
    }

    private static final class GroqHttpException extends RuntimeException {
        private final int statusCode;
        private GroqHttpException(int statusCode, String ignoredBody) { this.statusCode = statusCode; }
    }
}
