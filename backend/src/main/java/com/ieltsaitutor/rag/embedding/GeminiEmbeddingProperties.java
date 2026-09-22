package com.ieltsaitutor.rag.embedding;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.gemini")
public class GeminiEmbeddingProperties {
    private String apiKey = "";
    private String embeddingModel = "";
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models";
    private Duration responseTimeout = Duration.ofSeconds(20);
    private int maxRetries = 1;
    private int embeddingDimension = 768;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey == null ? "" : apiKey.trim(); }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel == null ? "" : embeddingModel.trim(); }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public Duration getResponseTimeout() { return responseTimeout; }
    public void setResponseTimeout(Duration responseTimeout) { this.responseTimeout = responseTimeout; }
    public int getMaxRetries() { return Math.max(0, Math.min(maxRetries, 1)); }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public int getEmbeddingDimension() { return embeddingDimension; }
    public void setEmbeddingDimension(int embeddingDimension) { this.embeddingDimension = embeddingDimension; }
}
