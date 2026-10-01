package com.ieltsaitutor.ai.config;

import com.ieltsaitutor.ai.provider.ProviderId;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "ai")
public class AiProviderProperties {
    private ProviderId primaryProvider = ProviderId.GROQ;
    private List<ProviderId> fallbackProviders = new ArrayList<>(List.of(ProviderId.CLOUDFLARE, ProviderId.GEMINI));
    private Health health = new Health();
    private Groq groq = new Groq();
    private Cloudflare cloudflare = new Cloudflare();

    public ProviderId getPrimaryProvider() { return primaryProvider; }
    public void setPrimaryProvider(ProviderId primaryProvider) { this.primaryProvider = primaryProvider; }
    public List<ProviderId> getFallbackProviders() { return List.copyOf(fallbackProviders); }
    public void setFallbackProviders(List<ProviderId> fallbackProviders) {
        this.fallbackProviders = fallbackProviders == null ? new ArrayList<>() : new ArrayList<>(fallbackProviders);
    }
    public Health getHealth() { return health; }
    public void setHealth(Health health) { this.health = health == null ? new Health() : health; }
    public Groq getGroq() { return groq; }
    public void setGroq(Groq groq) { this.groq = groq == null ? new Groq() : groq; }
    public Cloudflare getCloudflare() { return cloudflare; }
    public void setCloudflare(Cloudflare cloudflare) { this.cloudflare = cloudflare == null ? new Cloudflare() : cloudflare; }

    @AssertTrue(message = "AI provider order must not contain duplicate provider IDs")
    public boolean isProviderOrderUnique() {
        List<ProviderId> ordered = new ArrayList<>();
        if (primaryProvider != null) ordered.add(primaryProvider);
        ordered.addAll(fallbackProviders);
        return ordered.size() == ordered.stream().distinct().count();
    }

    public static class Health {
        @Min(1)
        private int transientFailureThreshold = 3;
        private Duration rollingWindow = Duration.ofSeconds(60);
        private Duration cooldown = Duration.ofSeconds(30);
        @Min(1)
        private int halfOpenProbes = 1;

        public int getTransientFailureThreshold() { return transientFailureThreshold; }
        public void setTransientFailureThreshold(int transientFailureThreshold) { this.transientFailureThreshold = transientFailureThreshold; }
        public Duration getRollingWindow() { return rollingWindow; }
        public void setRollingWindow(Duration rollingWindow) { this.rollingWindow = rollingWindow; }
        public Duration getCooldown() { return cooldown; }
        public void setCooldown(Duration cooldown) { this.cooldown = cooldown; }
        public int getHalfOpenProbes() { return halfOpenProbes; }
        public void setHalfOpenProbes(int halfOpenProbes) { this.halfOpenProbes = halfOpenProbes; }
    }

    public static class Groq {
        private String apiKey = "";
        private String chatModel = "";
        private String baseUrl = "https://api.groq.com/openai/v1";
        private Duration connectTimeout = Duration.ofSeconds(3);
        private Duration responseTimeout = Duration.ofSeconds(20);

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = normalize(apiKey); }
        public String getChatModel() { return chatModel; }
        public void setChatModel(String chatModel) { this.chatModel = normalize(chatModel); }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = normalize(baseUrl); }
        public Duration getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
        public Duration getResponseTimeout() { return responseTimeout; }
        public void setResponseTimeout(Duration responseTimeout) { this.responseTimeout = responseTimeout; }
        boolean isConfigured() { return !apiKey.isBlank() && !chatModel.isBlank() && !baseUrl.isBlank(); }
    }

    public static class Cloudflare {
        private String accountId = "";
        private String apiToken = "";
        private String chatModel = "";
        private String embeddingModel = "";
        private String baseUrl = "https://api.cloudflare.com/client/v4/accounts";
        private Duration connectTimeout = Duration.ofSeconds(3);
        private Duration responseTimeout = Duration.ofSeconds(20);

        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = normalize(accountId); }
        public String getApiToken() { return apiToken; }
        public void setApiToken(String apiToken) { this.apiToken = normalize(apiToken); }
        public String getChatModel() { return chatModel; }
        public void setChatModel(String chatModel) { this.chatModel = normalize(chatModel); }
        public String getEmbeddingModel() { return embeddingModel; }
        public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = normalize(embeddingModel); }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = normalize(baseUrl); }
        public Duration getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
        public Duration getResponseTimeout() { return responseTimeout; }
        public void setResponseTimeout(Duration responseTimeout) { this.responseTimeout = responseTimeout; }
        boolean isConfigured() {
            return !accountId.isBlank() && !apiToken.isBlank() && !baseUrl.isBlank()
                    && (!chatModel.isBlank() || !embeddingModel.isBlank());
        }
        String getDisplayId() { return accountId.isBlank() ? "cloudflare" : "cloudflare:" + accountId; }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
