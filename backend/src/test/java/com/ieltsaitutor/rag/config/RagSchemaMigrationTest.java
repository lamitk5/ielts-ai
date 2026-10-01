package com.ieltsaitutor.rag.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class RagSchemaMigrationTest {

    @Test
    void migrationResourceContainsVectorExtension() throws IOException {
        String sql = migrationSql();

        assertTrue(sql.contains("CREATE EXTENSION IF NOT EXISTS vector"));
    }

    @Test
    void migrationCreatesAllRagTables() throws IOException {
        String sql = migrationSql().toLowerCase();

        assertTrue(sql.contains("create table rag_documents"));
        assertTrue(sql.contains("create table rag_document_versions"));
        assertTrue(sql.contains("create table rag_chunks"));
        assertTrue(sql.contains("create table rag_ingestion_jobs"));
    }

    @Test
    void ragPropertiesBindLockedDefaults() {
        RagProperties properties = new RagProperties();

        assertEquals(768, properties.embeddingDimension());
        assertEquals(5, properties.topK());
        assertEquals(0.72d, properties.minSimilarity());
    }

    private String migrationSql() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__create_rag_schema.sql");
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
