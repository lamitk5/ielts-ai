package com.ieltsaitutor.rag.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingSpaceMigrationTest {
    @Test
    void migrationAddsCompleteSpaceMetadataAndMarksLegacyIndexesForReindex() throws IOException {
        String sql = new String(new ClassPathResource(
                "db/migration/V6__add_embedding_space_metadata.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8).toLowerCase();

        assertThat(sql).contains("embedding_provider", "embedding_model", "embedding_dimension", "embedding_version");
        assertThat(sql).contains("reindex_required");
        assertThat(sql).contains("index_status = 'reindex_required'");
        assertThat(sql).contains("where index_status = 'indexed'");
    }

    @Test
    void migrationRetainsChunksAndAddsExactSpaceIndexes() throws IOException {
        String sql = new String(new ClassPathResource(
                "db/migration/V6__add_embedding_space_metadata.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8).toLowerCase();

        assertThat(sql).contains("alter table rag_chunks", "create index");
        assertThat(sql).doesNotContain("delete from rag_chunks", "drop table");
    }
}
