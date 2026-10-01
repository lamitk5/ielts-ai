package com.ieltsaitutor.practice.generator.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.exception.SourceRightsException;
import com.ieltsaitutor.practice.generator.repository.PracticeGenerationRepository;
import com.ieltsaitutor.rag.domain.RightsStatus;

class SourceRightsGuardTest {

    private PracticeGenerationRepository repository;
    private SourceRightsGuard guard;

    @BeforeEach
    void setUp() {
        repository = mock(PracticeGenerationRepository.class);
        guard = new SourceRightsGuard(repository);
    }

    @Test
    void allowsApprovedSource() {
        UUID sourceId = UUID.randomUUID();
        PracticeGenerationSource source = new PracticeGenerationSource(
                sourceId, "Valid Approved Source", "PASTED_TEXT", "Author",
                RightsStatus.APPROVED, "Open Access CC-BY", "Content...", "hash",
                UUID.randomUUID(), Instant.now(), Instant.now());
        when(repository.findSourceById(sourceId)).thenReturn(Optional.of(source));

        assertDoesNotThrow(() -> guard.verifySourceCanGenerate(sourceId));
    }

    @Test
    void rejectsPendingReviewSource() {
        UUID sourceId = UUID.randomUUID();
        PracticeGenerationSource source = new PracticeGenerationSource(
                sourceId, "Pending Source", "PASTED_TEXT", "Author",
                RightsStatus.PENDING_REVIEW, "Awaiting clearance", "Content...", "hash",
                UUID.randomUUID(), Instant.now(), Instant.now());
        when(repository.findSourceById(sourceId)).thenReturn(Optional.of(source));

        assertThrows(SourceRightsException.class, () -> guard.verifySourceCanGenerate(sourceId));
    }

    @Test
    void rejectsRestrictedOrRejectedSource() {
        UUID restrictedId = UUID.randomUUID();
        PracticeGenerationSource restricted = new PracticeGenerationSource(
                restrictedId, "Internal Source", "PASTED_TEXT", "Author",
                RightsStatus.RESTRICTED, "Internal only", "Content...", "hash",
                UUID.randomUUID(), Instant.now(), Instant.now());
        when(repository.findSourceById(restrictedId)).thenReturn(Optional.of(restricted));

        assertThrows(SourceRightsException.class, () -> guard.verifySourceCanGenerate(restrictedId));

        UUID rejectedId = UUID.randomUUID();
        PracticeGenerationSource rejected = new PracticeGenerationSource(
                rejectedId, "Copyrighted Exam Bank", "PASTED_TEXT", "Commercial Press",
                RightsStatus.REJECTED, "Commercial copyright", "Content...", "hash",
                UUID.randomUUID(), Instant.now(), Instant.now());
        when(repository.findSourceById(rejectedId)).thenReturn(Optional.of(rejected));

        assertThrows(SourceRightsException.class, () -> guard.verifySourceCanGenerate(rejectedId));
    }

    @Test
    void rejectsNonExistentSource() {
        UUID missingId = UUID.randomUUID();
        when(repository.findSourceById(missingId)).thenReturn(Optional.empty());

        assertThrows(SourceRightsException.class, () -> guard.verifySourceCanGenerate(missingId));
    }
}
