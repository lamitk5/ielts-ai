package com.ieltsaitutor.ai.config;

import com.ieltsaitutor.ai.provider.ProviderCapability;
import com.ieltsaitutor.ai.provider.ProviderConfiguration;
import com.ieltsaitutor.ai.provider.ProviderId;
import com.ieltsaitutor.rag.embedding.GeminiEmbeddingProperties;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class ProviderConfigurationRegistry {
    private final AiProviderProperties providers;
    private final GeminiProperties gemini;
    private final GeminiEmbeddingProperties geminiEmbedding;

    public ProviderConfigurationRegistry(AiProviderProperties providers, GeminiProperties gemini,
            GeminiEmbeddingProperties geminiEmbedding) {
        this.providers = providers;
        this.gemini = gemini;
        this.geminiEmbedding = geminiEmbedding;
    }

    public ProviderConfiguration getConfiguration(ProviderId providerId) {
        return switch (providerId) {
            case GROQ -> configuration(ProviderId.GROQ, providers.getGroq().getChatModel(),
                    groqCapabilities(), "groq");
            case CLOUDFLARE -> configuration(ProviderId.CLOUDFLARE, cloudflareModel(),
                    cloudflareCapabilities(), providers.getCloudflare().getDisplayId());
            case GEMINI -> configuration(ProviderId.GEMINI, geminiModel(), geminiCapabilities(), "gemini");
        };
    }

    private Set<ProviderCapability> groqCapabilities() {
        if (!providers.getGroq().isConfigured()) return Set.of();
        return Set.of(ProviderCapability.CHAT);
    }

    private Set<ProviderCapability> cloudflareCapabilities() {
        if (!cloudflareConnectionConfigured()) return Set.of();
        EnumSet<ProviderCapability> capabilities = EnumSet.noneOf(ProviderCapability.class);
        if (!providers.getCloudflare().getChatModel().isBlank()) capabilities.add(ProviderCapability.CHAT);
        if (!providers.getCloudflare().getEmbeddingModel().isBlank()) capabilities.add(ProviderCapability.EMBEDDING);
        return capabilities;
    }

    private Set<ProviderCapability> geminiCapabilities() {
        if (gemini.getApiKey().isBlank() || gemini.getBaseUrl().isBlank()) return Set.of();
        EnumSet<ProviderCapability> capabilities = EnumSet.noneOf(ProviderCapability.class);
        if (!gemini.getModel().isBlank()) capabilities.add(ProviderCapability.CHAT);
        if (!geminiEmbedding.getEmbeddingModel().isBlank()) capabilities.add(ProviderCapability.EMBEDDING);
        return capabilities;
    }

    private boolean cloudflareConnectionConfigured() {
        AiProviderProperties.Cloudflare cloudflare = providers.getCloudflare();
        return !cloudflare.getAccountId().isBlank() && !cloudflare.getApiToken().isBlank()
                && !cloudflare.getBaseUrl().isBlank();
    }

    private String cloudflareModel() {
        return !providers.getCloudflare().getChatModel().isBlank()
                ? providers.getCloudflare().getChatModel()
                : providers.getCloudflare().getEmbeddingModel();
    }

    private String geminiModel() {
        return !gemini.getModel().isBlank() ? gemini.getModel() : geminiEmbedding.getEmbeddingModel();
    }

    private ProviderConfiguration configuration(ProviderId id, String model,
            Set<ProviderCapability> capabilities, String displayId) {
        return new ProviderConfiguration(id, !capabilities.isEmpty(), model, capabilities, displayId);
    }
}
