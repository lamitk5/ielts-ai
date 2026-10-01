package com.ieltsaitutor.rag.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("rag-postgres")
class RagRepositoryGovernanceIntegrationTest {

    @Test
    void excludesPendingRightsFromGovernedCandidates() {
        assertTrue(RagChunkJdbcRepository.GOVERNED_CANDIDATE_SQL.contains("rights_status = 'APPROVED'"));
    }

    @Test
    void excludesInactiveCurrentDocument() {
        assertTrue(RagChunkJdbcRepository.GOVERNED_CANDIDATE_SQL.contains("active = true"));
    }

    @Test
    void excludesNonIndexedVersion() {
        assertTrue(RagChunkJdbcRepository.GOVERNED_CANDIDATE_SQL.contains("index_status = 'INDEXED'"));
    }

    @Test
    void excludesVersionIndexedBeforeApproval() {
        assertTrue(RagChunkJdbcRepository.GOVERNED_CANDIDATE_SQL.contains("v.indexed_at >= v.approved_at"));
    }
}
