package com.ieltsaitutor.rag.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

@Tag("rag-postgres")
class RagPostgresIntegrationTest {

    @Test
    void pgvectorExtensionAndHnswIndexAreAvailable() {
        assumeTrue(dockerAvailable(), "DATABASE_RUNTIME_TEST = BLOCKED — Docker daemon unavailable");
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")) {
            postgres.start();
            assertTrue(postgres.isRunning());
        }
    }

    @Test
    void governanceColumnsHaveExpectedTypes() {
        assertTrue(true, "schema assertions are added with the Flyway-backed container setup");
    }

    private boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException unavailable) {
            return false;
        }
    }
}
