package com.ieltsaitutor.ai.provider;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.ieltsaitutor.ai.config.GeminiProperties;
import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.ChatHistoryItem;
import com.ieltsaitutor.ai.exception.AiProviderException;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Component
public class GeminiAiProvider implements AiProvider {
    private static final Logger log = LoggerFactory.getLogger(GeminiAiProvider.class);
    private final WebClient webClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiAiProvider(WebClient geminiWebClient, GeminiProperties properties, ObjectMapper objectMapper) {
        this.webClient = geminiWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiChatResult chat(AiChatCommand command) {
        if (properties.getApiKey().isBlank()) {
            throw new AiProviderException(
                    "AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    "Trợ giảng AI tạm thời chưa sẵn sàng.");
        }

        for (int attempt = 0; ; attempt++) {
            try {
                String responseBody = request(command);
                return AiChatResult.answered(extractAnswer(responseBody));
            } catch (GeminiHttpException exception) {
                if (isRetryable(exception.statusCode()) && attempt < properties.getMaxRetries()) {
                    pauseBeforeRetry(attempt);
                    continue;
                }
                throw mapHttpException(exception);
            } catch (JacksonException | IllegalArgumentException exception) {
                throw new AiProviderException(
                        "AI_PROVIDER_ERROR", HttpStatus.BAD_GATEWAY,
                        "AI provider returned an invalid response.", exception);
            } catch (WebClientRequestException exception) {
                if (isTimeout(exception)) {
                    throw timeoutException(exception);
                }
                throw new AiProviderException(
                        "AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                        "Trợ giảng AI tạm thời chưa sẵn sàng.", exception);
            } catch (IllegalStateException exception) {
                if (exception.getMessage() != null && exception.getMessage().contains("Timeout")) {
                    throw timeoutException(exception);
                }
                throw new AiProviderException(
                        "AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                        "Trợ giảng AI tạm thời chưa sẵn sàng.", exception);
            }
        }
    }

    private String request(AiChatCommand command) {
        String uri = properties.getBaseUrl().replaceAll("/$", "") + "/" + properties.getModel() + ":generateContent";
        return webClient.post()
                .uri(uri)
                .header("x-goog-api-key", properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildRequest(command))
                .exchangeToMono(response -> response.bodyToMono(String.class).defaultIfEmpty("")
                        .flatMap(body -> response.statusCode().isError()
                                ? reactor.core.publisher.Mono.error(new GeminiHttpException(response.statusCode().value(), body))
                                : reactor.core.publisher.Mono.just(body)))
                .block(properties.getResponseTimeout());
    }

    private String buildRequest(AiChatCommand command) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode systemInstruction = root.putObject("systemInstruction");
        systemInstruction.putArray("parts").addObject().put("text", TutorSystemInstruction.TEXT);
        ArrayNode contents = root.putArray("contents");
        for (ChatHistoryItem historyItem : command.history()) {
            addContent(contents, historyItem.role().equals("ASSISTANT") ? "model" : "user", historyItem.content());
        }
        addContent(contents, "user", buildUserPrompt(command));
        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.putObject("thinkingConfig").put("thinkingLevel", "low");
        generationConfig.put("temperature", 0.4);
        return root.toString();
    }

    private String buildUserPrompt(AiChatCommand command) {
        AiChatContext context = command.context();
        StringBuilder prompt = new StringBuilder(command.message());
        StringBuilder contextText = new StringBuilder();
        addContext(contextText, "skill", context.normalizedSkill());
        addContext(contextText, "lessonId", context.lessonId());
        addContext(contextText, "exerciseId", context.exerciseId());
        addContext(contextText, "questionId", context.questionId());
        addContext(contextText, "taskType", context.taskType());
        addContext(contextText, "errorLocation", context.errorLocation());
        addContext(contextText, "selectedText", context.selectedText());
        if (!contextText.isEmpty()) {
            prompt.append("\n\nLearning context supplied by the application:\n").append(contextText);
        }
        return prompt.toString();
    }

    private void addContext(StringBuilder builder, String label, String value) {
        if (value != null && !value.isBlank()) builder.append(label).append('=').append(value).append('\n');
    }

    private void addContent(ArrayNode contents, String role, String text) {
        ObjectNode content = contents.addObject();
        content.put("role", role);
        content.putArray("parts").addObject().put("text", text);
    }

    private String extractAnswer(String responseBody) throws JacksonException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (!text.isTextual() || text.asText().isBlank()) {
            throw new IllegalArgumentException("missing answer text");
        }
        return text.asText().trim();
    }

    private boolean isRetryable(int statusCode) {
        return statusCode == 429 || statusCode == 503 || statusCode >= 500;
    }

    private void pauseBeforeRetry(int attempt) {
        try {
            Thread.sleep(50L * (1L << attempt));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    "Trợ giảng AI tạm thời chưa sẵn sàng.", interrupted);
        }
    }

    private AiProviderException mapHttpException(GeminiHttpException exception) {
        if (exception.statusCode() == 429) {
            log.warn("Gemini provider rate limit status={}", exception.statusCode());
            return new AiProviderException("AI_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS,
                    "Trợ giảng AI đang nhận quá nhiều yêu cầu. Vui lòng thử lại sau.");
        }
        if (exception.statusCode() == 503) {
            log.warn("Gemini provider unavailable status={}", exception.statusCode());
            return new AiProviderException("AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    "Trợ giảng AI tạm thời chưa sẵn sàng.");
        }
        log.warn("Gemini provider error status={}", exception.statusCode());
        return new AiProviderException("AI_PROVIDER_ERROR", HttpStatus.BAD_GATEWAY,
                "Trợ giảng AI chưa thể trả lời lúc này.");
    }

    private boolean isTimeout(WebClientRequestException exception) {
        return exception.getCause() instanceof java.util.concurrent.TimeoutException
                || exception.getMessage() != null && exception.getMessage().toLowerCase().contains("timeout");
    }

    private AiProviderException timeoutException(Throwable cause) {
        return new AiProviderException("AI_TIMEOUT", HttpStatus.GATEWAY_TIMEOUT,
                "Kết nối tới Trợ giảng AI đã hết thời gian. Vui lòng thử lại.", cause);
    }

    private static final class GeminiHttpException extends RuntimeException {
        private final int statusCode;
        private final String providerBody;

        private GeminiHttpException(int statusCode, String providerBody) {
            super("Gemini provider status " + statusCode);
            this.statusCode = statusCode;
            this.providerBody = providerBody;
        }

        private int statusCode() { return statusCode; }
    }
}
