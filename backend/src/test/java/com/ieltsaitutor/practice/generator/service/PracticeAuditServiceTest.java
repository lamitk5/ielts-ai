package com.ieltsaitutor.practice.generator.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.generator.domain.PracticeReviewAction;
import com.ieltsaitutor.practice.generator.repository.PracticeGenerationRepository;

class PracticeAuditServiceTest {

    private PracticeGenerationRepository repository;
    private PracticeAuditService auditService;

    @BeforeEach
    void setUp() {
        repository = mock(PracticeGenerationRepository.class);
        auditService = new PracticeAuditService(repository);
    }

    @Test
    void recordActionSavesAndReturnsReviewAction() {
        UUID setId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        when(repository.saveReviewAction(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PracticeReviewAction action = auditService.recordAction(setId, versionId, adminId, "APPROVE", "Passed quality criteria");

        assertNotNull(action);
        assertEquals(setId, action.setId());
        assertEquals(versionId, action.versionId());
        assertEquals(adminId, action.adminId());
        assertEquals("APPROVE", action.action());
        assertEquals("Passed quality criteria", action.reviewerNotes());
        verify(repository).saveReviewAction(any(PracticeReviewAction.class));
    }

    @Test
    void listActionsDelegatesToRepository() {
        UUID setId = UUID.randomUUID();
        PracticeReviewAction a = new PracticeReviewAction(UUID.randomUUID(), setId, UUID.randomUUID(), UUID.randomUUID(), "REJECT", "Low quality", Instant.now());
        when(repository.listReviewActionsForSet(setId)).thenReturn(List.of(a));

        List<PracticeReviewAction> actions = auditService.listActions(setId);
        assertEquals(1, actions.size());
        assertEquals("REJECT", actions.get(0).action());
    }
}
