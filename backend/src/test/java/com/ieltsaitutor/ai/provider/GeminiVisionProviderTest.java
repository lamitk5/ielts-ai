package com.ieltsaitutor.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.ieltsaitutor.ai.attachment.AttachmentKind;
import com.ieltsaitutor.ai.config.GeminiProperties;
import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.exception.AiProviderException;
import com.ieltsaitutor.ai.model.AiAttachmentPart;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.ObjectMapper;

class GeminiVisionProviderTest {
    private HttpServer server;
    private AtomicReference<String> body;
    private GeminiProperties properties;
    private GeminiAiProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        body = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        properties = new GeminiProperties();
        properties.setApiKey("server-only-test-key");
        properties.setModel("gemini-test");
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/v1beta/models");
        properties.setResponseTimeout(Duration.ofSeconds(2));
        properties.setMaxRetries(0);
        provider = new GeminiAiProvider(WebClient.builder().build(), properties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() { server.stop(0); }

    @Test
    void sendsActualImagePartNotFilenameOnly() {
        properties.setVisionEnabled(true);
        startWithAnswer();
        byte[] pixels = new byte[] { 1, 2, 3, 4 };
        AiChatCommand command = imageCommand(pixels);

        provider.chat(command);

        assertThat(body.get()).contains("\"inlineData\"");
        assertThat(body.get()).contains("\"mimeType\":\"image/png\"");
        assertThat(body.get()).contains(Base64.getEncoder().encodeToString(pixels));
    }

    @Test
    void preservesQuestionAndTrustedContext() {
        properties.setVisionEnabled(true);
        startWithAnswer();

        provider.chat(new AiChatCommand("What is visible?",
                new AiChatContext("WRITING", "lesson", null, null, "TASK_1", null, "trusted selection"),
                List.of(), "request", "trusted evidence", List.of(imagePart(new byte[] { 9 })),
                Set.of(ProviderCapability.VISION_IMAGE)));

        assertThat(body.get()).contains("What is visible?", "trusted selection", "trusted evidence");
    }

    @Test
    void rejectsVisionWhenCapabilityDisabled() {
        assertThat(provider.capabilities()).doesNotContain(ProviderCapability.VISION_IMAGE);

        assertThatThrownBy(() -> provider.chat(imageCommand(new byte[] { 1 })))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("Vision input is not enabled for the configured AI provider.");
    }

    @Test
    void textOnlyRequestPayloadRemainsCompatible() {
        startWithAnswer();

        provider.chat(new AiChatCommand("hello", new AiChatContext("GENERAL", null, null, null, null, null, null), List.of()));

        assertThat(body.get()).contains("\"text\":\"hello");
        assertThat(body.get()).doesNotContain("inlineData");
    }

    private AiChatCommand imageCommand(byte[] bytes) {
        return new AiChatCommand("Describe the image", new AiChatContext("GENERAL", null, null, null, null, null, null),
                List.of(), "request", null, List.of(imagePart(bytes)), Set.of(ProviderCapability.VISION_IMAGE));
    }

    private AiAttachmentPart imagePart(byte[] bytes) {
        return new AiAttachmentPart(UUID.randomUUID(), "photo.png", "image/png", AttachmentKind.IMAGE,
                () -> new ByteArrayInputStream(bytes));
    }

    private void startWithAnswer() {
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> respond(exchange,
                "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"ok\"}]}}]}"));
        server.start();
    }

    private void respond(HttpExchange exchange, String response) throws IOException {
        body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }
}
