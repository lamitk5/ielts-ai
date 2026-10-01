package com.ieltsaitutor.rag.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class EmbeddingProviderContractTest {
    @Test
    void separatesDocumentAndQueryTasks() {
        CapturingProvider provider = new CapturingProvider();

        provider.embed(new EmbeddingRequest("document text", EmbeddingTask.DOCUMENT));
        provider.embed(new EmbeddingRequest("query text", EmbeddingTask.QUERY));

        assertThat(provider.requests).extracting(EmbeddingRequest::task)
                .containsExactly(EmbeddingTask.DOCUMENT, EmbeddingTask.QUERY);
    }

    @Test
    void rejectsWrongVectorDimension() {
        assertThatThrownBy(() -> new EmbeddingVector(768, List.of(0.1f)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimension");
    }

    private static final class CapturingProvider implements EmbeddingProvider {
        private final java.util.ArrayList<EmbeddingRequest> requests = new java.util.ArrayList<>();

        @Override
        public EmbeddingResult embed(EmbeddingRequest request) {
            requests.add(request);
            return new EmbeddingResult("test", 1, List.of(1f));
        }

        @Override
        public List<EmbeddingResult> embedBatch(List<EmbeddingRequest> requests) {
            return requests.stream().map(this::embed).toList();
        }
    }
}
