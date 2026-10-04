package com.ieltsaitutor.rag.ingestion;

public record ChunkingOptions(int targetTokens, int overlapTokens, int mergeThresholdTokens) {
    public ChunkingOptions {
        if (targetTokens <= 0 || overlapTokens < 0 || mergeThresholdTokens <= 0) {
            throw new IllegalArgumentException("Chunking options must be positive and overlap cannot be negative");
        }
        if (overlapTokens >= targetTokens) {
            throw new IllegalArgumentException("Overlap must be smaller than target size");
        }
    }

    public static ChunkingOptions defaults() {
        return new ChunkingOptions(550, 80, 120);
    }
}
