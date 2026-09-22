package com.ieltsaitutor.rag.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import com.ieltsaitutor.rag.domain.IngestionJobStatus;
import com.ieltsaitutor.rag.domain.RagChunk;
import com.ieltsaitutor.rag.domain.RagDocument;
import com.ieltsaitutor.rag.domain.RagDocumentVersion;
import com.ieltsaitutor.rag.domain.RagIngestionJob;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.domain.Skill;

class RagRepositoryTest {

    @Test
    void storesDocumentWithPendingRights() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        RagDocumentJdbcRepository repository = new RagDocumentJdbcRepository(jdbc);
        RagDocument document = new RagDocument(UUID.randomUUID(), "Writing guide", "PROJECT_CREATED", null,
                null, "en", Skill.WRITING, RightsStatus.PENDING_REVIEW, "Created by project", false, null,
                Instant.now(), Instant.now());

        repository.create(document);

        verify(jdbc).update(contains("INSERT INTO rag_documents"), org.mockito.ArgumentMatchers.any(MapSqlParameterSource.class));
    }

    @Test
    void rejectsDuplicateDocumentVersion() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        doThrow(new DuplicateKeyException("duplicate version")).when(jdbc)
                .update(contains("INSERT INTO rag_document_versions"),
                        org.mockito.ArgumentMatchers.any(MapSqlParameterSource.class));
        RagDocumentVersionJdbcRepository repository = new RagDocumentVersionJdbcRepository(jdbc);
        RagDocumentVersion version = new RagDocumentVersion(UUID.randomUUID(), UUID.randomUUID(), "1.0", "guide.txt",
                "text/plain", 10L, "a".repeat(64), "doc/version/file.bin",
                com.ieltsaitutor.rag.domain.ExtractionStatus.PENDING,
                com.ieltsaitutor.rag.domain.IndexStatus.NOT_INDEXED,
                null, null, Instant.now());

        assertThrows(DuplicateKeyException.class, () -> repository.createVersion(version));
    }

    @Test
    void storesNullablePageAndSection() {
        RagChunk chunk = new RagChunk(UUID.randomUUID(), UUID.randomUUID(), 0, "content", null, null, 2,
                List.of(), Map.of(), Instant.now());

        assertEquals(null, chunk.pageNumber());
        assertEquals(null, chunk.sectionTitle());
    }

    @Test
    void recordsJobFailureSafely() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        RagIngestionJobJdbcRepository repository = new RagIngestionJobJdbcRepository(jdbc);
        RagIngestionJob job = new RagIngestionJob(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                IngestionJobStatus.FAILED, "RAG_EMBEDDING_FAILED", "Embedding provider unavailable", null,
                Instant.now(), Instant.now());

        repository.updateStatus(job);

        verify(jdbc).update(contains("UPDATE rag_ingestion_jobs"), org.mockito.ArgumentMatchers.any(MapSqlParameterSource.class));
    }
}
