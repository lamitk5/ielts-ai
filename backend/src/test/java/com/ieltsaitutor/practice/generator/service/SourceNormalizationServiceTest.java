package com.ieltsaitutor.practice.generator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.generator.exception.SourceNormalizationException;
import com.ieltsaitutor.rag.domain.Skill;

class SourceNormalizationServiceTest {

    private SourceNormalizationService service;

    @BeforeEach
    void setUp() {
        service = new SourceNormalizationService();
    }

    @Test
    void normalizesCleanTextAndComputesChecksum() {
        String rawText = "<p>This is a <b>scientific study</b> on marine ecosystems.</p>\n<script>alert(1)</script>\r\n"
                + "The research evaluates sub-polar microplastic accumulation across estuaries. ".repeat(30);

        String normalized = service.normalizeText(rawText);
        assertFalse(normalized.contains("<p>"));
        assertFalse(normalized.contains("<b>"));
        assertFalse(normalized.contains("<script>"));
        assertFalse(normalized.contains("alert(1)"));

        String checksum = service.computeChecksum(normalized);
        assertNotNull(checksum);
        assertEquals(64, checksum.length());
    }

    @Test
    void validatesReadingWordCountBoundaries() {
        String shortText = "Too short passage for reading.";
        assertThrows(SourceNormalizationException.class, () -> service.validateWordCount(shortText, Skill.READING));

        String validText = "Academic reading paragraph exploring complex biogeochemical cycles. ".repeat(60); // ~420 words
        int wordCount = service.countWords(validText);
        assertTrue(wordCount >= 400 && wordCount <= 1200);
        service.validateWordCount(validText, Skill.READING);

        String longText = "Very long passage with repeated academic vocabulary. ".repeat(250); // ~1500 words
        assertThrows(SourceNormalizationException.class, () -> service.validateWordCount(longText, Skill.READING));
    }
}
