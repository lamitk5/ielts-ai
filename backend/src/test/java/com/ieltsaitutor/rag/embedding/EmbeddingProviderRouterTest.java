package com.ieltsaitutor.rag.embedding;

import com.ieltsaitutor.ai.provider.ProviderId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddingProviderRouterTest {
    private final EmbeddingSpace cloudflareSpace = new EmbeddingSpace(ProviderId.CLOUDFLARE, "bge", 768, "v1");
    private final EmbeddingSpace geminiSpace = new EmbeddingSpace(ProviderId.GEMINI, "gemini-embedding-2", 768, "v1");

    @Test
    void selectsCloudflareAsPrimary() {
        FakeProvider cloudflare = new FakeProvider(ProviderId.CLOUDFLARE, cloudflareSpace, true);
        FakeProvider gemini = new FakeProvider(ProviderId.GEMINI, geminiSpace, true);
        EmbeddingProviderRouter router = router(cloudflare, gemini);

        EmbeddingResult result = router.embed(new EmbeddingRequest("hello", EmbeddingTask.QUERY));

        assertThat(result.space()).isEqualTo(cloudflareSpace);
        assertThat(cloudflare.calls).isEqualTo(1);
        assertThat(gemini.calls).isZero();
    }

    @Test
    void selectsGeminiOnlyWhenTheRequestedIndexSpaceIsGemini() {
        FakeProvider cloudflare = new FakeProvider(ProviderId.CLOUDFLARE, cloudflareSpace, true);
        FakeProvider gemini = new FakeProvider(ProviderId.GEMINI, geminiSpace, true);

        EmbeddingResult result = router(cloudflare, gemini)
                .embed(new EmbeddingRequest("hello", EmbeddingTask.QUERY, geminiSpace));

        assertThat(result.space()).isEqualTo(geminiSpace);
        assertThat(cloudflare.calls).isZero();
        assertThat(gemini.calls).isEqualTo(1);
    }

    @Test
    void doesNotCrossSpacesWhenRequestedProviderIsDisabled() {
        FakeProvider cloudflare = new FakeProvider(ProviderId.CLOUDFLARE, cloudflareSpace, false);
        FakeProvider gemini = new FakeProvider(ProviderId.GEMINI, geminiSpace, true);

        assertThatThrownBy(() -> router(cloudflare, gemini)
                .embed(new EmbeddingRequest("hello", EmbeddingTask.QUERY, cloudflareSpace)))
                .isInstanceOf(RagEmbeddingException.class)
                .hasMessageContaining("khả dụng");
    }

    private EmbeddingProviderRouter router(FakeProvider cloudflare, FakeProvider gemini) {
        return new EmbeddingProviderRouter(List.of(cloudflare, gemini), new EmbeddingSpaceSelector(List.of(cloudflare, gemini)));
    }

    private static final class FakeProvider implements EmbeddingProvider {
        private final ProviderId id;
        private final EmbeddingSpace space;
        private final boolean configured;
        private int calls;

        private FakeProvider(ProviderId id, EmbeddingSpace space, boolean configured) {
            this.id = id;
            this.space = space;
            this.configured = configured;
        }

        @Override public ProviderId providerId() { return id; }
        @Override public EmbeddingSpace embeddingSpace() { return space; }
        @Override public boolean isEmbeddingConfigured() { return configured; }
        @Override public EmbeddingResult embed(EmbeddingRequest request) {
            calls++;
            return new EmbeddingResult(space.model(), 768,
                    IntStream.range(0, 768).mapToObj(i -> 0f).toList(), space);
        }
        @Override public List<EmbeddingResult> embedBatch(List<EmbeddingRequest> requests) {
            return requests.stream().map(this::embed).toList();
        }
    }
}
