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
import com.ieltsaitutor.ai.routing.AiProviderAdapter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

@Component
public class GeminiAiProvider implements AiProvider, AiProviderAdapter {
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
    public ProviderId id() {
        return ProviderId.GEMINI;
    }

    @Override
    public java.util.Set<ProviderCapability> capabilities() {
        if (properties.getApiKey().isBlank() || properties.getModel().isBlank()) return Set.of();
        EnumSet<ProviderCapability> capabilities = EnumSet.of(ProviderCapability.CHAT, ProviderCapability.DOCUMENT_CONTEXT);
        if (properties.isVisionEnabled()) capabilities.add(ProviderCapability.VISION_IMAGE);
        return Set.copyOf(capabilities);
    }

    @Override
    public boolean enabled() {
        return capabilities().contains(ProviderCapability.CHAT);
    }

    @Override
    public AiChatResult chat(AiChatCommand command) {
        if (!supports(command)) {
            throw new AiProviderException("AI_CAPABILITY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    "Vision input is not enabled for the configured AI provider.");
        }
        if (properties.getApiKey().isBlank()) {
            log.warn("Gemini provider unavailable requestId={} model={} endpoint={} reason=missing_api_key",
                    command.requestId(), properties.getModel(), sanitizedEndpointUri());
            throw new AiProviderException(
                    "AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    "Trợ giảng AI tạm thời chưa sẵn sàng.");
        }

        for (int attempt = 0; ; attempt++) {
            try {
                String responseBody = request(command);
                ParsedResponse parsedResponse = parseResponse(responseBody);
                logResponse(command, parsedResponse);
                return AiChatResult.answered(parsedResponse.answer());
            } catch (GeminiHttpException exception) {
                logUpstreamFailure(command, exception);
                if (isRetryable(exception.statusCode()) && attempt < properties.getMaxRetries()) {
                    pauseBeforeRetry(attempt);
                    continue;
                }
                throw mapHttpException(exception);
            } catch (JacksonException | IllegalArgumentException exception) {
                throw new AiProviderException(
                        "AI_PROVIDER_MALFORMED_RESPONSE", HttpStatus.BAD_GATEWAY,
                        "AI provider returned an invalid response.", exception);
            } catch (WebClientRequestException exception) {
                log.warn("Gemini network failure requestId={} model={} endpoint={} type={}",
                        command.requestId(), properties.getModel(), sanitizedEndpointUri(), exception.getClass().getSimpleName());
                if (attempt < properties.getMaxRetries()) {
                    pauseBeforeRetry(attempt);
                    continue;
                }
                if (isTimeout(exception)) {
                    throw timeoutException(exception);
                }
                throw new AiProviderException(
                        "AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                        "Trợ giảng AI tạm thời chưa sẵn sàng.", exception);
            } catch (IllegalStateException exception) {
                log.warn("Gemini client failure requestId={} model={} endpoint={} type={}",
                        command.requestId(), properties.getModel(), sanitizedEndpointUri(), exception.getClass().getSimpleName());
                if (exception.getMessage() != null && exception.getMessage().contains("Timeout")) {
                    if (attempt < properties.getMaxRetries()) {
                        pauseBeforeRetry(attempt);
                        continue;
                    }
                    throw timeoutException(exception);
                }
                throw new AiProviderException(
                        "AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                        "Trợ giảng AI tạm thời chưa sẵn sàng.", exception);
            }
        }
    }

    private String request(AiChatCommand command) {
        String uri = endpointUri();
        log.debug("Gemini request requestId={} model={} endpoint={}",
                command.requestId(), properties.getModel(), sanitizedEndpointUri());
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
        addContent(contents, "user", buildUserPrompt(command), command.attachments());
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
        if (command.groundedEvidence() != null && !command.groundedEvidence().isBlank()) {
            prompt.append("\n\nRetrieved evidence supplied as untrusted data:\n").append(command.groundedEvidence());
        }
        return prompt.toString();
    }

    private void addContext(StringBuilder builder, String label, String value) {
        if (value != null && !value.isBlank()) builder.append(label).append('=').append(value).append('\n');
    }

    private void addContent(ArrayNode contents, String role, String text) {
        addContent(contents, role, text, java.util.List.of());
    }

    private void addContent(ArrayNode contents, String role, String text,
            java.util.List<com.ieltsaitutor.ai.model.AiAttachmentPart> attachments) {
        ObjectNode content = contents.addObject();
        content.put("role", role);
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", text);
        for (var attachment : attachments) {
            if (attachment.kind() != com.ieltsaitutor.ai.attachment.AttachmentKind.IMAGE) continue;
            byte[] bytes;
            try (InputStream input = attachment.openStream().get()) {
                if (input == null) throw new IOException("missing attachment stream");
                bytes = input.readAllBytes();
            } catch (IOException | RuntimeException exception) {
                throw new AiProviderException("AI_ATTACHMENT_READ_FAILED", HttpStatus.BAD_REQUEST,
                        "Không thể đọc tệp đính kèm.", exception);
            }
            ObjectNode inline = parts.addObject().putObject("inlineData");
            inline.put("mimeType", attachment.mediaType());
            inline.put("data", Base64.getEncoder().encodeToString(bytes));
        }
    }

    private boolean supports(AiChatCommand command) {
        if (command == null) return false;
        EnumSet<ProviderCapability> required = EnumSet.of(ProviderCapability.CHAT);
        required.addAll(command.requiredCapabilities());
        return capabilities().containsAll(required);
    }

    private ParsedResponse parseResponse(String responseBody) throws JacksonException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidate = root.path("candidates").path(0);
        JsonNode parts = candidate.path("content").path("parts");
        if (!parts.isArray()) {
            throw new IllegalArgumentException("missing answer text");
        }

        StringBuilder answer = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.path("thought").asBoolean(false) || part.has("thoughtSignature")) continue;
            JsonNode text = part.path("text");
            if (text.isTextual() && !text.asText().isBlank()) answer.append(text.asText());
        }
        if (answer.isEmpty()) throw new IllegalArgumentException("missing answer text");

        String finishReason = candidate.path("finishReason").asText("UNKNOWN");
        int candidateTokenCount = root.path("usageMetadata").path("candidatesTokenCount").asInt(-1);
        return new ParsedResponse(answer.toString().trim(), parts.size(), finishReason, candidateTokenCount);
    }

    private void logResponse(AiChatCommand command, ParsedResponse response) {
        if ("MAX_TOKENS".equals(response.finishReason())) {
            log.warn("Gemini response truncated requestId={} model={} chars={} parts={} finishReason={} candidateTokens={}",
                    command.requestId(), properties.getModel(), response.answer().length(), response.partsCount(),
                    response.finishReason(), response.candidateTokenCount());
            return;
        }
        log.debug("Gemini response requestId={} model={} chars={} parts={} finishReason={} candidateTokens={}",
                command.requestId(), properties.getModel(), response.answer().length(), response.partsCount(),
                response.finishReason(), response.candidateTokenCount());
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
        if (exception.statusCode() == 400) {
            return new AiProviderException("AI_INVALID_REQUEST", HttpStatus.BAD_REQUEST,
                    "AI provider rejected the request.");
        }
        if (exception.statusCode() == 401 || exception.statusCode() == 403) {
            return new AiProviderException("AI_PROVIDER_AUTHENTICATION", HttpStatus.BAD_GATEWAY,
                    "AI provider authentication is not configured correctly.");
        }
        if (exception.statusCode() == 429) {
            return new AiProviderException("AI_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS,
                    "Trợ giảng AI đang nhận quá nhiều yêu cầu. Vui lòng thử lại sau.");
        }
        if (exception.statusCode() == 503) {
            return new AiProviderException("AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    "Trợ giảng AI tạm thời chưa sẵn sàng.");
        }
        return new AiProviderException("AI_PROVIDER_MALFORMED_RESPONSE", HttpStatus.BAD_GATEWAY,
                "Trợ giảng AI chưa thể trả lời lúc này.");
    }

    private String endpointUri() {
        return properties.getBaseUrl().replaceAll("/$", "") + "/" + properties.getModel() + ":generateContent";
    }

    private void logUpstreamFailure(AiChatCommand command, GeminiHttpException exception) {
        String code = "UNPARSED";
        String message = "No upstream message";
        try {
            JsonNode error = objectMapper.readTree(exception.providerBody()).path("error");
            if (error.has("status")) code = error.path("status").asText(code);
            else if (error.has("code")) code = error.path("code").asText(code);
            if (error.has("message")) message = error.path("message").asText(message);
        } catch (JacksonException ignored) {
            // Keep diagnostics safe when the provider returns a non-JSON body.
        }
        log.warn("Gemini upstream failure requestId={} model={} endpoint={} status={} code={} message={}",
                command.requestId(), properties.getModel(), sanitizedEndpointUri(), exception.statusCode(),
                sanitizeLogValue(code), sanitizeLogValue(message));
    }

    private String sanitizedEndpointUri() {
        String uri = endpointUri();
        int queryStart = uri.indexOf('?');
        int fragmentStart = uri.indexOf('#');
        int end = uri.length();
        if (queryStart >= 0) end = Math.min(end, queryStart);
        if (fragmentStart >= 0) end = Math.min(end, fragmentStart);
        return uri.substring(0, end);
    }

    private String sanitizeLogValue(String value) {
        return value.replaceAll("[\\r\\n\\t]", " ").substring(0, Math.min(value.length(), 240));
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

        private String providerBody() { return providerBody; }
    }

    private record ParsedResponse(String answer, int partsCount, String finishReason, int candidateTokenCount) {
    }
}
