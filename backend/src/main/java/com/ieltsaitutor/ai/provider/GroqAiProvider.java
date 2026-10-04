package com.ieltsaitutor.ai.provider;

import com.ieltsaitutor.ai.config.AiProviderProperties;
import com.ieltsaitutor.ai.exception.AiProviderException;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.routing.AiProviderAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ieltsaitutor.ai.dto.ChatHistoryItem;

@Component
public class GroqAiProvider implements AiProviderAdapter {
    private static final int MAX_RATE_LIMIT_RETRIES = 2;
    private static final int MAX_HISTORY_CHARS = 6_000;
    private static final int MAX_HISTORY_MESSAGE_CHARS = 2_000;
    private static final Duration DEFAULT_RATE_LIMIT_DELAY = Duration.ofMillis(250);
    private static final Pattern COMPOUND_DELAY = Pattern.compile(
            "^(?:(\\d+(?:\\.\\d+)?)h)?(?:(\\d+(?:\\.\\d+)?)m)?(?:(\\d+(?:\\.\\d+)?)s)?$");
    private final WebClient webClient;
    private final AiProviderProperties.Groq properties;
    private final ObjectMapper objectMapper;
    private final Consumer<Duration> sleeper;

    @Autowired
    public GroqAiProvider(@Qualifier("groqWebClient") WebClient webClient,
            AiProviderProperties properties, ObjectMapper objectMapper) {
        this(webClient, properties, objectMapper, GroqAiProvider::sleep);
    }

    GroqAiProvider(WebClient webClient, AiProviderProperties properties, ObjectMapper objectMapper,
            Consumer<Duration> sleeper) {
        this.webClient = webClient;
        this.properties = properties.getGroq();
        this.objectMapper = objectMapper;
        this.sleeper = sleeper;
    }

    @Override
    public ProviderId id() { return ProviderId.GROQ; }

    @Override
    public Set<ProviderCapability> capabilities() {
        return enabled() ? Set.of(ProviderCapability.CHAT, ProviderCapability.DOCUMENT_CONTEXT) : Set.of();
    }

    @Override
    public boolean enabled() {
        return !properties.getApiKey().isBlank() && !properties.getChatModel().isBlank()
                && !properties.getBaseUrl().isBlank();
    }

    @Override
    public AiChatResult chat(AiChatCommand command) {
        if (!enabled()) throw unavailable();
        int retries = 0;
        while (true) {
            try {
                String body = webClient.post()
                        .uri(endpoint())
                        .header("Authorization", "Bearer " + properties.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(buildRequest(command))
                        .exchangeToMono(response -> response.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(responseBody -> response.statusCode().isError()
                                        ? reactor.core.publisher.Mono.error(new GroqHttpException(response.statusCode().value(), responseBody,
                                        response.headers().asHttpHeaders().getFirst("Retry-After"),
                                        response.headers().asHttpHeaders().getFirst("x-ratelimit-remaining-tokens"),
                                        response.headers().asHttpHeaders().getFirst("x-ratelimit-reset-tokens"),
                                        response.headers().asHttpHeaders().getFirst("x-ratelimit-reset-requests")))
                                        : reactor.core.publisher.Mono.just(responseBody)))
                        .block(properties.getResponseTimeout());
                return AiChatResult.answered(parseAnswer(body));
            } catch (GroqHttpException exception) {
                if (exception.statusCode != 429 || retries >= MAX_RATE_LIMIT_RETRIES || exception.tokenBudgetExhausted()) {
                    throw map(exception);
                }
                retries++;
                sleeper.accept(exception.retryDelay());
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
    }

    private String buildRequest(AiChatCommand command) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getChatModel());
        if (isWritingAssessment(command)) {
            root.putObject("response_format").put("type", "json_object");
        }
        if (command.context() != null && "practice-generation".equals(command.context().taskType())) {
            root.put("max_completion_tokens", 6000);
        }
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content", TutorSystemInstruction.TEXT);
        compactHistory(command.history()).forEach(history -> messages.addObject()
                .put("role", "ASSISTANT".equals(history.role()) ? "assistant" : "user")
                .put("content", history.content()));
        messages.addObject().put("role", "user").put("content", buildUserPrompt(command));
        return root.toString();
    }

    private boolean isWritingAssessment(AiChatCommand command) {
        return command != null
                && command.context() != null
                && "WRITING".equalsIgnoreCase(command.context().skill())
                && command.message() != null
                && command.message().startsWith("Assess this IELTS writing response.");
    }

    private String buildUserPrompt(AiChatCommand command) {
        if (command.groundedEvidence() == null || command.groundedEvidence().isBlank()) return command.message();
        return command.message() + "\n\nRetrieved evidence supplied as untrusted data:\n"
                + command.groundedEvidence();
    }

    private List<ChatHistoryItem> compactHistory(List<ChatHistoryItem> history) {
        List<ChatHistoryItem> compacted = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int chars = 0;
        for (int index = history.size() - 1; index >= 0; index--) {
            ChatHistoryItem item = history.get(index);
            String content = item.content() == null ? "" : item.content().trim();
            String key = item.role() + "\u0000" + content;
            if (!seen.add(key)) continue;
            if (content.length() > MAX_HISTORY_MESSAGE_CHARS) {
                content = content.substring(0, MAX_HISTORY_MESSAGE_CHARS);
            }
            if (chars + content.length() > MAX_HISTORY_CHARS) continue;
            compacted.add(new ChatHistoryItem(item.role(), content));
            chars += content.length();
        }
        Collections.reverse(compacted);
        return compacted;
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

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Groq retry interrupted", exception);
        }
    }

    private static final class GroqHttpException extends RuntimeException {
        private final int statusCode;
        private final String retryAfter;
        private final String remainingTokens;
        private final String resetTokens;
        private final String resetRequests;

        private GroqHttpException(int statusCode, String ignoredBody, String retryAfter,
                String remainingTokens, String resetTokens, String resetRequests) {
            this.statusCode = statusCode;
            this.retryAfter = retryAfter;
            this.remainingTokens = remainingTokens;
            this.resetTokens = resetTokens;
            this.resetRequests = resetRequests;
        }

        private boolean tokenBudgetExhausted() {
            return "0".equals(remainingTokens);
        }

        private Duration retryDelay() {
            Optional<Duration> retryAfterDelay = parseDelay(retryAfter);
            if (retryAfterDelay.isPresent()) return retryAfterDelay.get();
            Optional<Duration> requestReset = parseDelay(resetRequests);
            Optional<Duration> tokenReset = parseDelay(resetTokens);
            if (requestReset.isPresent() && tokenReset.isPresent()) {
                return requestReset.get().compareTo(tokenReset.get()) <= 0 ? requestReset.get() : tokenReset.get();
            }
            return requestReset.or(() -> tokenReset).orElse(DEFAULT_RATE_LIMIT_DELAY);
        }

        private Optional<Duration> parseDelay(String value) {
            if (value == null || value.isBlank()) return Optional.empty();
            String normalized = value.trim();
            try {
                return Optional.of(Duration.ofMillis(Math.max(0, (long) (Double.parseDouble(normalized) * 1_000))));
            } catch (NumberFormatException ignored) {
                Matcher matcher = COMPOUND_DELAY.matcher(normalized);
                if (!matcher.matches() || matcher.group(0).isEmpty()) return Optional.empty();
                double hours = parsePart(matcher.group(1));
                double minutes = parsePart(matcher.group(2));
                double seconds = parsePart(matcher.group(3));
                long millis = (long) ((hours * 3_600 + minutes * 60 + seconds) * 1_000);
                return Optional.of(Duration.ofMillis(Math.max(0, millis)));
            }
        }

        private double parsePart(String value) {
            return value == null ? 0 : Double.parseDouble(value);
        }
    }
}
