package com.ieltsaitutor.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.catalog.ApprovedPracticeCatalogService;
import com.ieltsaitutor.practice.catalog.PracticePublication;

class SubmissionOwnershipAndVersionTest {
    @Test
    void resolvesOnlyTheActivePublicationAndPinsItsRevisionAndVersion() {
        ApprovedPracticeCatalogService catalog = mock(ApprovedPracticeCatalogService.class);
        UUID generatedSetId = UUID.randomUUID();
        UUID generatedVersionId = UUID.randomUUID();
        when(catalog.findActive("published-reading-1")).thenReturn(java.util.Optional.of(new PracticePublication(
                "published-reading-1", generatedSetId, generatedVersionId, "reading", true, 4, "approved", Instant.now())));

        SubmissionPracticeResolver resolver = new SubmissionPracticeResolver(catalog);

        ResolvedPracticeVersion resolved = resolver.resolve("published-reading-1", "reading");

        assertEquals("published-reading-1", resolved.publishedSetId());
        assertEquals(generatedVersionId.toString(), resolved.practiceVersionId());
        assertEquals(4, resolved.publicationRevision());
    }

    @Test
    void rejectsInactiveUnknownOrWrongSkillPublication() {
        ApprovedPracticeCatalogService catalog = mock(ApprovedPracticeCatalogService.class);
        when(catalog.findActive("missing")).thenReturn(java.util.Optional.empty());
        when(catalog.findActive("published-writing-1")).thenReturn(java.util.Optional.of(new PracticePublication(
                "published-writing-1", UUID.randomUUID(), UUID.randomUUID(), "writing", true, 1, "approved", Instant.now())));
        SubmissionPracticeResolver resolver = new SubmissionPracticeResolver(catalog);

        assertThrows(SubmissionConflictException.class, () -> resolver.resolve("missing", "reading"));
        assertThrows(SubmissionConflictException.class, () -> resolver.resolve("published-writing-1", "reading"));
    }

    @Test
    void ownershipCheckDoesNotRevealAnotherLearnersSubmission() {
        UUID owner = UUID.randomUUID();
        PracticeSubmission submission = new PracticeSubmission(UUID.randomUUID(), owner, "READING", "set",
                "version-1", "published", 1, SubmissionStatus.DRAFT, Instant.now(), null, null, null, 0,
                "start-key", null, null, false, Instant.now(), Instant.now());
        SubmissionOwnershipService ownership = new SubmissionOwnershipService();

        assertEquals(submission, ownership.requireOwner(owner, submission));
        assertThrows(SubmissionConflictException.class,
                () -> ownership.requireOwner(UUID.randomUUID(), submission));
    }
}
