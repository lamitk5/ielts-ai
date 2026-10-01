package com.ieltsaitutor.ai.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AiTutorReferenceTest {
    @Test
    void createsValidImmutableReadingReference() {
        AiTutorReference ref = new AiTutorReference(
                "PASSAGE", "reading-foundation-01-p1",
                null, "p1", null, null, null, null, "INFO", "Đoạn 1 bài đọc", null, null
        );

        assertEquals("PASSAGE", ref.referenceType());
        assertEquals("reading-foundation-01-p1", ref.targetId());
        assertEquals("p1", ref.paragraphId());
        assertEquals("INFO", ref.severity());
        assertEquals("Đoạn 1 bài đọc", ref.label());
        assertTrue(ref.isValid());
    }

    @Test
    void createsValidVersionBoundWritingDraftReference() {
        AiTutorReference ref = new AiTutorReference(
                "DRAFT", "writing-editor",
                null, null, null, 10, 45, null, "SUGGESTION", "Cần cải thiện ngữ pháp", null, 2L
        );

        assertEquals("DRAFT", ref.referenceType());
        assertEquals("writing-editor", ref.targetId());
        assertEquals(10, ref.startOffset());
        assertEquals(45, ref.endOffset());
        assertEquals(2L, ref.draftVersion());
        assertTrue(ref.isValid());
    }

    @Test
    void rejectsMaliciousDomCommandsAndSelectors() {
        AiTutorReference cssRef = new AiTutorReference(
                "PASSAGE", "div.passage > p:nth-child(2)",
                null, null, null, null, null, null, null, null, null, null
        );
        assertFalse(cssRef.isValid());

        AiTutorReference xpathRef = new AiTutorReference(
                "PASSAGE", "//div[@id='passage']",
                null, null, null, null, null, null, null, null, null, null
        );
        assertFalse(xpathRef.isValid());

        AiTutorReference scriptRef = new AiTutorReference(
                "PASSAGE", "<script>alert(1)</script>",
                null, null, null, null, null, null, null, null, null, null
        );
        assertFalse(scriptRef.isValid());
    }

    @Test
    void rejectsInvalidOffsetsOrMissingDraftVersion() {
        // startOffset > endOffset
        AiTutorReference invertedOffsets = new AiTutorReference(
                "DRAFT", "writing-editor",
                null, null, null, 50, 10, null, null, null, null, 1L
        );
        assertFalse(invertedOffsets.isValid());

        // Negative offset
        AiTutorReference negativeOffset = new AiTutorReference(
                "DRAFT", "writing-editor",
                null, null, null, -5, 10, null, null, null, null, 1L
        );
        assertFalse(negativeOffset.isValid());

        // DRAFT with offset but missing draftVersion / contentVersion
        AiTutorReference unversionedDraft = new AiTutorReference(
                "DRAFT", "writing-editor",
                null, null, null, 10, 20, null, null, null, null, null
        );
        assertFalse(unversionedDraft.isValid());
    }
}
