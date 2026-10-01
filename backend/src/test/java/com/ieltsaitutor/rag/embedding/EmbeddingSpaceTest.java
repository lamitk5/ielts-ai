package com.ieltsaitutor.rag.embedding;

import com.ieltsaitutor.ai.provider.ProviderId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddingSpaceTest {
    @Test
    void equalityAndStableKeyIncludeEverySpaceField() {
        EmbeddingSpace first = new EmbeddingSpace(ProviderId.CLOUDFLARE, "bge", 768, "v1");

        assertThat(first).isEqualTo(new EmbeddingSpace(ProviderId.CLOUDFLARE, "bge", 768, "v1"));
        assertThat(first.key()).isEqualTo("CLOUDFLARE:bge:768:v1");
        assertThat(first).isNotEqualTo(new EmbeddingSpace(ProviderId.GEMINI, "bge", 768, "v1"));
        assertThat(first).isNotEqualTo(new EmbeddingSpace(ProviderId.CLOUDFLARE, "other", 768, "v1"));
        assertThat(first).isNotEqualTo(new EmbeddingSpace(ProviderId.CLOUDFLARE, "bge", 768, "v2"));
    }

    @Test
    void onlyThePhysicalVectorDimensionSupportedByThisPhaseIsAccepted() {
        assertThatThrownBy(() -> new EmbeddingSpace(ProviderId.CLOUDFLARE, "bge", 1536, "v1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("768");
    }

    @Test
    void matchesOnlyTheExactProviderModelDimensionAndVersion() {
        EmbeddingSpace space = new EmbeddingSpace(ProviderId.GEMINI, "gemini-embedding-2", 768, "v1");

        assertThat(space.matches(new EmbeddingSpace(ProviderId.GEMINI, "gemini-embedding-2", 768, "v1"))).isTrue();
        assertThat(space.matches(new EmbeddingSpace(ProviderId.GEMINI, "other", 768, "v1"))).isFalse();
        assertThat(space.matches(new EmbeddingSpace(ProviderId.GEMINI, "gemini-embedding-2", 768, "v2"))).isFalse();
    }
}
