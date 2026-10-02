package com.ieltsaitutor.writing;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WritingSubmissionVersionTest {

    @Test
    @DisplayName("Create first immutable writing submission version")
    void createsFirstVersionSuccessfully() {
        UUID id = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        Instant now = Instant.now();
        String text = "This is a test essay with several words.";

        WritingSubmissionVersion version = new WritingSubmissionVersion(
                id,
                submissionId,
                1,
                null,
                text,
                8,
                "some-hash",
                now);

        assertEquals(id, version.id());
        assertEquals(submissionId, version.submissionId());
        assertEquals(1, version.versionNumber());
        assertNull(version.parentVersionId());
        assertEquals(text, version.responseText());
        assertEquals(8, version.wordCount());
        assertEquals("some-hash", version.contentHash());
        assertEquals(now, version.createdAt());
    }

    @Test
    @DisplayName("Invalid version arguments throw IllegalArgumentException")
    void rejectsInvalidArguments() {
        UUID id = UUID.randomUUID();
        UUID subId = UUID.randomUUID();
        Instant now = Instant.now();

        assertThrows(NullPointerException.class, () -> new WritingSubmissionVersion(null, subId, 1, null, "text", 1, "h", now));
        assertThrows(NullPointerException.class, () -> new WritingSubmissionVersion(id, null, 1, null, "text", 1, "h", now));
        assertThrows(IllegalArgumentException.class, () -> new WritingSubmissionVersion(id, subId, 0, null, "text", 1, "h", now));
        assertThrows(IllegalArgumentException.class, () -> new WritingSubmissionVersion(id, subId, 1, null, null, 0, "h", now));
        assertThrows(IllegalArgumentException.class, () -> new WritingSubmissionVersion(id, subId, 1, null, "text", -1, "h", now));
    }
}
