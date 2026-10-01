package com.ieltsaitutor.rag.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.rag.repository.RagChunkJdbcRepository;

@Tag("rag-postgres")
class VectorRetrievalServiceIntegrationTest {
    @Test
    void executesCosineQueryAgainstPgvector() {
        assertThat(RagChunkJdbcRepository.GOVERNED_CANDIDATE_SQL)
                .contains("<=> CAST(:embedding AS vector)", "1 -");
    }

    @Test
    void returnsStableSourceMetadata() {
        assertThat(RagChunkJdbcRepository.GOVERNED_CANDIDATE_SQL)
                .contains("document_title", "document_version", "source_id");
    }
}
