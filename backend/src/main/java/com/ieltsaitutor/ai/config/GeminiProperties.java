package com.ieltsaitutor.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "google.gemini")
public class GeminiProperties {
    private String apiKey = "";
    private String model = "gemini-3.8-flash";
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models";
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration responseTimeout = Duration.ofSeconds(20);
    private int maxRetries = 2;
    private boolean visionEnabled;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey == null ? "" : apiKey.trim(); }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getResponseTimeout() { return responseTimeout; }
    public void setResponseTimeout(Duration responseTimeout) { this.responseTimeout = responseTimeout; }
    public int getMaxRetries() { return Math.max(0, Math.min(maxRetries, 2)); }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public boolean isVisionEnabled() { return visionEnabled; }
    public void setVisionEnabled(boolean visionEnabled) { this.visionEnabled = visionEnabled; }

}
