package com.ieltsaitutor.rag.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public class RagProperties {
    private String storageRoot = "backend/data/rag/uploads";
    private long maxUploadBytes = 10 * 1024 * 1024L;
    private int embeddingDimension = 768;
    private String embeddingVersion = "v1";
    private int topK = 5;
    private double minSimilarity = 0.72d;
    private int embeddingBatchSize = 32;

    public Path storageRootPath() {
        return Path.of(storageRoot);
    }

    public String storageRoot() { return storageRoot; }
    public void setStorageRoot(String storageRoot) { this.storageRoot = storageRoot; }
    public long maxUploadBytes() { return maxUploadBytes; }
    public void setMaxUploadBytes(long maxUploadBytes) { this.maxUploadBytes = maxUploadBytes; }
    public int embeddingDimension() { return embeddingDimension; }
    public void setEmbeddingDimension(int embeddingDimension) { this.embeddingDimension = embeddingDimension; }
    public String embeddingVersion() { return embeddingVersion; }
    public void setEmbeddingVersion(String embeddingVersion) { this.embeddingVersion = embeddingVersion == null ? "" : embeddingVersion.trim(); }
    public int topK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public double minSimilarity() { return minSimilarity; }
    public void setMinSimilarity(double minSimilarity) { this.minSimilarity = minSimilarity; }
    public int embeddingBatchSize() { return embeddingBatchSize; }
    public void setEmbeddingBatchSize(int embeddingBatchSize) { this.embeddingBatchSize = embeddingBatchSize; }
}
