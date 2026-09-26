package com.ieltsaitutor.practice.generator.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeSet;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeVersion;
import com.ieltsaitutor.practice.generator.domain.GenerationState;
import com.ieltsaitutor.practice.generator.domain.PracticeReviewAction;
import com.ieltsaitutor.practice.generator.dto.GeneratedSetReviewPayload;
import com.ieltsaitutor.practice.generator.dto.ReviewActionRequest;
import com.ieltsaitutor.practice.generator.dto.ReviewActionResponse;
import com.ieltsaitutor.practice.generator.exception.InvalidReviewActionException;
import com.ieltsaitutor.practice.generator.repository.PracticeGenerationRepository;
import com.ieltsaitutor.rag.domain.Skill;

class PracticeReviewServiceTest {

    private PracticeGenerationRepository repository;
    private PracticeAuditService auditService;
    private PracticeBankHydrationService hydrationService;
    private GenerationStateMachine stateMachine;
    private DefaultPracticeReviewService reviewService;

    @BeforeEach
    void setUp() {
        repository = mock(PracticeGenerationRepository.class);
        auditService = mock(PracticeAuditService.class);
        hydrationService = mock(PracticeBankHydrationService.class);
        stateMachine = new GenerationStateMachine();

        reviewService = new DefaultPracticeReviewService(
                repository, auditService, hydrationService, stateMachine
        );
    }

    @Test
    void approveReadyForReviewSetSucceedsAndHydrates() {
        UUID setId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        GeneratedPracticeSet set = new GeneratedPracticeSet(
                setId, UUID.randomUUID(), Skill.READING, "Climate Passage",
                versionId, GenerationState.PENDING_REVIEW, null, null, null,
                Instant.now(), Instant.now()
        );
        GeneratedPracticeVersion version = new GeneratedPracticeVersion(
                versionId, setId, 1, "Passage text", "[]", "{}", Instant.now()
        );

        when(repository.findSetById(setId)).thenReturn(Optional.of(set));
        when(repository.findVersionById(versionId)).thenReturn(Optional.of(version));
        when(hydrationService.hydrate(eq(set), eq(version), eq(adminId))).thenReturn("reading-climate-passage-1");
        when(auditService.recordAction(eq(setId), eq(versionId), eq(adminId), eq("APPROVE"), any()))
                .thenReturn(new PracticeReviewAction(UUID.randomUUID(), setId, versionId, adminId, "APPROVE", "Great", Instant.now()));

        ReviewActionResponse response = reviewService.executeReview(
                setId, new ReviewActionRequest("APPROVE", "Great set", null, null), adminId
        );

        assertNotNull(response);
        assertEquals("APPROVE", response.action());
        assertEquals(GenerationState.APPROVED, response.resultingState());
        verify(repository).markSetApproved(setId, "reading-climate-passage-1", adminId);
        verify(hydrationService).hydrate(set, version, adminId);
    }

    @Test
    void requestRevisionTransitionsToNeedsRevision() {
        UUID setId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        GeneratedPracticeSet set = new GeneratedPracticeSet(
                setId, UUID.randomUUID(), Skill.READING, "Climate Passage",
                versionId, GenerationState.PENDING_REVIEW, null, null, null,
                Instant.now(), Instant.now()
        );

        when(repository.findSetById(setId)).thenReturn(Optional.of(set));
        when(auditService.recordAction(eq(setId), eq(versionId), eq(adminId), eq("REQUEST_REVISION"), any()))
                .thenReturn(new PracticeReviewAction(UUID.randomUUID(), setId, versionId, adminId, "REQUEST_REVISION", "Fix Q3", Instant.now()));

        ReviewActionResponse response = reviewService.executeReview(
                setId, new ReviewActionRequest("REQUEST_REVISION", "Fix Q3", "Make question 3 clearer", "3"), adminId
        );

        assertEquals(GenerationState.NEEDS_REVISION, response.resultingState());
        verify(repository).updateSetState(setId, GenerationState.NEEDS_REVISION, versionId);
    }

    @Test
    void rejectTransitionsToRejected() {
        UUID setId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        GeneratedPracticeSet set = new GeneratedPracticeSet(
                setId, UUID.randomUUID(), Skill.READING, "Climate Passage",
                versionId, GenerationState.PENDING_REVIEW, null, null, null,
                Instant.now(), Instant.now()
        );

        when(repository.findSetById(setId)).thenReturn(Optional.of(set));
        when(auditService.recordAction(eq(setId), eq(versionId), eq(adminId), eq("REJECT"), any()))
                .thenReturn(new PracticeReviewAction(UUID.randomUUID(), setId, versionId, adminId, "REJECT", "Low quality", Instant.now()));

        ReviewActionResponse response = reviewService.executeReview(
                setId, new ReviewActionRequest("REJECT", "Low quality", null, null), adminId
        );

        assertEquals(GenerationState.REJECTED, response.resultingState());
        verify(repository).updateSetState(setId, GenerationState.REJECTED, versionId);
    }

    @Test
    void approvingGeneratingSetThrowsException() {
        UUID setId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        GeneratedPracticeSet set = new GeneratedPracticeSet(
                setId, UUID.randomUUID(), Skill.READING, "Climate Passage",
                null, GenerationState.GENERATING, null, null, null,
                Instant.now(), Instant.now()
        );

        when(repository.findSetById(setId)).thenReturn(Optional.of(set));

        assertThrows(InvalidReviewActionException.class, () ->
                reviewService.executeReview(setId, new ReviewActionRequest("APPROVE", null, null, null), adminId)
        );
    }

    @Test
    void getReviewPayloadReturnsCompleteAggregate() {
        UUID setId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        GeneratedPracticeSet set = new GeneratedPracticeSet(
                setId, jobId, Skill.READING, "Climate Passage",
                versionId, GenerationState.PENDING_REVIEW, null, null, null,
                Instant.now(), Instant.now()
        );
        GeneratedPracticeVersion version = new GeneratedPracticeVersion(
                versionId, setId, 1, "Passage text", "[]", "{}", Instant.now()
        );

        when(repository.findSetById(setId)).thenReturn(Optional.of(set));
        when(repository.findVersionById(versionId)).thenReturn(Optional.of(version));
        when(repository.listReviewActionsForSet(setId)).thenReturn(List.of());
        when(repository.listVersionsForSet(setId)).thenReturn(List.of(version));

        GeneratedSetReviewPayload payload = reviewService.getReviewPayload(setId);

        assertNotNull(payload);
        assertEquals(set, payload.practiceSet());
        assertEquals(version, payload.currentVersion());
        assertEquals(1, payload.versionHistory().size());
    }
}
