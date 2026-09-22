package com.ieltsaitutor.ai.config;

import com.ieltsaitutor.ai.provider.ProviderCapability;
import com.ieltsaitutor.ai.provider.ProviderConfiguration;
import com.ieltsaitutor.ai.provider.ProviderId;
import com.ieltsaitutor.rag.embedding.GeminiEmbeddingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsEnvironmentDerivedProviderSettingsWithoutExposingCredentials() {
        contextRunner.withPropertyValues(
                        "ai.primary-provider=GROQ",
                        "ai.fallback-providers=CLOUDFLARE,GEMINI",
                        "ai.health.transient-failure-threshold=3",
                        "ai.health.rolling-window=60s",
                        "ai.health.cooldown=30s",
                        "ai.health.half-open-probes=1",
                        "ai.groq.api-key=groq-test-secret",
                        "ai.groq.chat-model=groq-chat-test",
                        "ai.groq.base-url=https://groq.example/v1",
                        "ai.groq.connect-timeout=4s",
                        "ai.groq.response-timeout=21s",
                        "ai.cloudflare.account-id=account-test",
                        "ai.cloudflare.api-token=cloudflare-test-secret",
                        "ai.cloudflare.chat-model=cloudflare-chat-test",
                        "ai.cloudflare.embedding-model=cloudflare-embedding-test",
                        "ai.cloudflare.base-url=https://cloudflare.example/client/v4/accounts",
                        "ai.cloudflare.connect-timeout=5s",
                        "ai.cloudflare.response-timeout=22s",
                        "google.gemini.api-key=gemini-test-secret",
                        "google.gemini.model=gemini-3.8-flash",
                        "google.gemini.base-url=https://gemini.example/models",
                        "google.gemini.connect-timeout=6s",
                        "google.gemini.response-timeout=23s",
                        "google.gemini.max-retries=0",
                        "google.gemini.embedding-model=gemini-embedding-test",
                        "rag.embedding-dimension=768")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AiProviderProperties properties = context.getBean(AiProviderProperties.class);
                    GeminiProperties gemini = context.getBean(GeminiProperties.class);
                    ProviderConfigurationRegistry configurations = context.getBean(ProviderConfigurationRegistry.class);

                    assertThat(properties.getPrimaryProvider()).isEqualTo(ProviderId.GROQ);
                    assertThat(properties.getFallbackProviders()).containsExactly(ProviderId.CLOUDFLARE, ProviderId.GEMINI);
                    assertThat(properties.getHealth().getTransientFailureThreshold()).isEqualTo(3);
                    assertThat(properties.getHealth().getRollingWindow()).isEqualTo(Duration.ofSeconds(60));
                    assertThat(properties.getHealth().getCooldown()).isEqualTo(Duration.ofSeconds(30));
                    assertThat(properties.getHealth().getHalfOpenProbes()).isEqualTo(1);
                    assertThat(properties.getGroq().getConnectTimeout()).isEqualTo(Duration.ofSeconds(4));
                    assertThat(properties.getGroq().getResponseTimeout()).isEqualTo(Duration.ofSeconds(21));
                    assertThat(properties.getCloudflare().getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
                    assertThat(properties.getCloudflare().getResponseTimeout()).isEqualTo(Duration.ofSeconds(22));
                    assertThat(gemini.getModel()).isEqualTo("gemini-3.8-flash");
                    assertThat(gemini.getConnectTimeout()).isEqualTo(Duration.ofSeconds(6));
                    assertThat(gemini.getResponseTimeout()).isEqualTo(Duration.ofSeconds(23));
                    assertThat(gemini.getMaxRetries()).isZero();
                    assertThat(configurations.getConfiguration(ProviderId.GROQ))
                            .isEqualTo(new ProviderConfiguration(ProviderId.GROQ, true, "groq-chat-test",
                                    java.util.Set.of(ProviderCapability.CHAT), "groq"));
                    assertThat(configurations.getConfiguration(ProviderId.CLOUDFLARE))
                            .isEqualTo(new ProviderConfiguration(ProviderId.CLOUDFLARE, true, "cloudflare-chat-test",
                                    java.util.Set.of(ProviderCapability.CHAT, ProviderCapability.EMBEDDING), "cloudflare:account-test"));
                    assertThat(configurations.getConfiguration(ProviderId.GEMINI))
                            .isEqualTo(new ProviderConfiguration(ProviderId.GEMINI, true, "gemini-3.8-flash",
                                    java.util.Set.of(ProviderCapability.CHAT, ProviderCapability.EMBEDDING), "gemini"));
                    assertThat(Arrays.stream(ProviderConfiguration.class.getRecordComponents())
                            .map(component -> component.getName()))
                            .containsExactly("id", "enabled", "model", "capabilities", "displayId");
                    assertThat(configurations.getConfiguration(ProviderId.GROQ).toString())
                            .doesNotContain("groq-test-secret", "cloudflare-test-secret", "gemini-test-secret");
                    assertThat(context.getEnvironment().getProperty("rag.embedding-dimension")).isEqualTo("768");
                    assertThat(properties.toString()).doesNotContain("groq-test-secret", "cloudflare-test-secret");
                });
    }

    @Test
    void reportsOnlyCloudflareEmbeddingCapabilityWhenOnlyTheEmbeddingModelIsConfigured() {
        contextRunner.withPropertyValues(
                        "ai.cloudflare.account-id=account-test",
                        "ai.cloudflare.api-token=cloudflare-test-secret",
                        "ai.cloudflare.embedding-model=cloudflare-embedding-test")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    assertThat(context.getBean(ProviderConfigurationRegistry.class).getConfiguration(ProviderId.CLOUDFLARE))
                            .isEqualTo(new ProviderConfiguration(ProviderId.CLOUDFLARE, true, "cloudflare-embedding-test",
                                    java.util.Set.of(ProviderCapability.EMBEDDING), "cloudflare:account-test"));
                });
    }

    @Test
    void omitsGeminiEmbeddingCapabilityWhenTheEmbeddingModelIsBlank() {
        contextRunner.withPropertyValues(
                        "google.gemini.api-key=gemini-test-secret",
                        "google.gemini.embedding-model=")
                .run(context -> assertThat(context.getBean(ProviderConfigurationRegistry.class)
                        .getConfiguration(ProviderId.GEMINI).capabilities())
                        .containsExactly(ProviderCapability.CHAT));
    }

    @Test
    void rejectsDuplicateProviderIdsInTheConfiguredOrder() {
        contextRunner.withPropertyValues(
                        "ai.primary-provider=GROQ",
                        "ai.fallback-providers=GROQ,GEMINI")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("AI provider order must not contain duplicate provider IDs");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({AiProviderProperties.class, GeminiProperties.class, GeminiEmbeddingProperties.class})
    @Import(ProviderConfigurationRegistry.class)
    static class PropertiesConfiguration {
    }
}
