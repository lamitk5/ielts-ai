package com.ieltsaitutor.ai.config;

import com.ieltsaitutor.IeltsAiTutorApplication;
import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.ai.exception.AiProviderException;
import com.ieltsaitutor.ai.provider.ProviderId;
import com.ieltsaitutor.ai.service.AiChatService;
import com.ieltsaitutor.rag.chat.RagChatService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ApplicationStartupWithoutOptionalProvidersTest {
    @Test
    void startsTheBackendWithGeminiOnlyConfiguration() {
        try (ConfigurableApplicationContext context = start("GEMINI_API_KEY=gemini-test-key")) {
            ProviderConfigurationRegistry configurations = context.getBean(ProviderConfigurationRegistry.class);

            assertThat(configurations.getConfiguration(ProviderId.GEMINI).enabled()).isTrue();
            assertThat(configurations.getConfiguration(ProviderId.GROQ).enabled()).isFalse();
            assertThat(configurations.getConfiguration(ProviderId.CLOUDFLARE).enabled()).isFalse();
        }
    }

    @Test
    void startsTheBackendWithGroqOnlyConfiguration() {
        try (ConfigurableApplicationContext context = start(
                "GROQ_API_KEY=groq-test-key", "GROQ_CHAT_MODEL=groq-chat-test")) {
            ProviderConfigurationRegistry configurations = context.getBean(ProviderConfigurationRegistry.class);

            assertThat(configurations.getConfiguration(ProviderId.GROQ).enabled()).isTrue();
            assertThat(configurations.getConfiguration(ProviderId.GEMINI).enabled()).isFalse();
            assertThat(configurations.getConfiguration(ProviderId.CLOUDFLARE).enabled()).isFalse();
        }
    }

    @Test
    void startsTheBackendWithCloudflareOnlyConfiguration() {
        try (ConfigurableApplicationContext context = start(
                "CLOUDFLARE_ACCOUNT_ID=account-test", "CLOUDFLARE_API_TOKEN=cloudflare-test-token",
                "CLOUDFLARE_CHAT_MODEL=cloudflare-chat-test")) {
            ProviderConfigurationRegistry configurations = context.getBean(ProviderConfigurationRegistry.class);

            assertThat(configurations.getConfiguration(ProviderId.CLOUDFLARE).enabled()).isTrue();
            assertThat(configurations.getConfiguration(ProviderId.GROQ).enabled()).isFalse();
            assertThat(configurations.getConfiguration(ProviderId.GEMINI).enabled()).isFalse();
        }
    }

    @Test
    void startsTheBackendWithNoConfiguredProviderAndFailsRequestsWithoutNetworkCalls() {
        try (ConfigurableApplicationContext context = start()) {
            ProviderConfigurationRegistry configurations = context.getBean(ProviderConfigurationRegistry.class);
            assertThat(configurations.getConfiguration(ProviderId.GROQ).enabled()).isFalse();
            assertThat(configurations.getConfiguration(ProviderId.CLOUDFLARE).enabled()).isFalse();
            assertThat(configurations.getConfiguration(ProviderId.GEMINI).enabled()).isFalse();

            assertThatThrownBy(() -> context.getBean(AiChatService.class)
                    .chat(new AiChatRequest("hello", null, null)))
                    .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                        assertThat(exception.code()).isEqualTo("AI_TEMPORARILY_UNAVAILABLE");
                        assertThat(exception.status().value()).isEqualTo(503);
                    });
        }
    }

    private ConfigurableApplicationContext start(String... properties) {
        String[] defaults = {
                "spring.main.banner-mode=off",
                "RAG_FLYWAY_ENABLED=false",
                "rag.cli=",
                "GEMINI_API_KEY=",
                "GROQ_API_KEY=",
                "GROQ_CHAT_MODEL=",
                "CLOUDFLARE_ACCOUNT_ID=",
                "CLOUDFLARE_API_TOKEN=",
                "CLOUDFLARE_CHAT_MODEL=",
                "CLOUDFLARE_EMBEDDING_MODEL=",
                "google.gemini.api-key=",
                "google.gemini.embedding-model=",
                "ai.groq.api-key=",
                "ai.groq.chat-model=",
                "ai.cloudflare.account-id=",
                "ai.cloudflare.api-token=",
                "ai.cloudflare.chat-model=",
                "ai.cloudflare.embedding-model="
        };
        String[] combined = java.util.stream.Stream.concat(java.util.Arrays.stream(defaults), java.util.Arrays.stream(properties))
                .toArray(String[]::new);
        java.util.Map<String, String> previous = new java.util.HashMap<>();
        for (String property : combined) {
            int separator = property.indexOf('=');
            if (separator <= 0) continue;
            String key = property.substring(0, separator);
            if (!key.endsWith("_API_KEY") && !key.startsWith("CLOUDFLARE_")
                    && !key.startsWith("GROQ_") && !key.startsWith("GEMINI_")) continue;
            previous.put(key, System.getProperty(key));
            System.setProperty(key, property.substring(separator + 1));
        }
        try {
            return new SpringApplicationBuilder(IeltsAiTutorApplication.class, TestInfrastructureConfiguration.class)
                    .web(WebApplicationType.NONE)
                    .properties(combined)
                    .run();
        } finally {
            previous.forEach((key, value) -> {
                if (value == null) System.clearProperty(key);
                else System.setProperty(key, value);
            });
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestInfrastructureConfiguration {
        @Bean
        @Primary
        DataSource dataSource() {
            return mock(DataSource.class);
        }

        @Bean
        @Primary
        RagChatService ragChatService() {
            return command -> null;
        }
    }
}
