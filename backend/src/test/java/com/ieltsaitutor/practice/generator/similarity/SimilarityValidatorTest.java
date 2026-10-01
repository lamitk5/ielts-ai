package com.ieltsaitutor.practice.generator.similarity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.generator.ai.RawPassagePayload;
import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;
import com.ieltsaitutor.practice.generator.validator.ValidationReport;
import com.ieltsaitutor.rag.domain.RightsStatus;

class SimilarityValidatorTest {

    private SimilarityValidator validator;

    @BeforeEach
    void setUp() {
        SimilarityPolicyRegistry registry = new SimilarityPolicyRegistry();
        NGramOverlapCalculator nGramCalc = new NGramOverlapCalculator();
        ContiguousSequenceDetector seqDetector = new ContiguousSequenceDetector();
        EntityRetentionFilter entityFilter = new EntityRetentionFilter();
        validator = new SimilarityValidator(registry, nGramCalc, seqDetector, entityFilter);
    }

    @Test
    void passesWhenTextsAreNovelAndIndependent() {
        PracticeGenerationSource source = new PracticeGenerationSource(
                UUID.randomUUID(), "Source Title", "PASTED_TEXT", "Author",
                RightsStatus.APPROVED, "License",
                "The history of renewable wind energy in Northern Europe dates back several centuries to early mill designs.",
                "hash", UUID.randomUUID(), Instant.now(), Instant.now());

        RawPracticePackage pkg = new RawPracticePackage(
                "Marine Biotechnology",
                new RawPassagePayload("Marine Biotechnology", List.of(
                        new RawPassagePayload.RawParagraphPayload("p1", "Deep-sea hydrothermal ecosystems host extremophilic organisms producing novel enzymes.")
                )),
                List.of(), "model", "v1", Instant.now());

        ValidationReport report = validator.validate(pkg, null, source);
        assertNotNull(report);
        assertEquals(ValidationStatus.PASS, report.status());
        assertEquals("similarity-policy-v1.0-conservative", report.policyVersion());
    }

    @Test
    void failsWhenLongContiguousSequenceIsCopied() {
        String copiedSentence = "Recent surveys in high latitude estuaries reveal unexpected microplastic accumulation across benthic layers and intertidal mudflats.";
        PracticeGenerationSource source = new PracticeGenerationSource(
                UUID.randomUUID(), "Source Title", "PASTED_TEXT", "Author",
                RightsStatus.APPROVED, "License",
                copiedSentence + " Additional source details follow.",
                "hash", UUID.randomUUID(), Instant.now(), Instant.now());

        RawPracticePackage pkg = new RawPracticePackage(
                "Arctic Estuaries",
                new RawPassagePayload("Arctic Estuaries", List.of(
                        new RawPassagePayload.RawParagraphPayload("p1", copiedSentence + " Some different conclusion.")
                )),
                List.of(), "model", "v1", Instant.now());

        ValidationReport report = validator.validate(pkg, null, source);
        assertNotNull(report);
        assertEquals(ValidationStatus.FAIL, report.status());
        assertTrue(report.findings().stream().anyMatch(f -> f.code().contains("CONTIGUOUS_SEQUENCE_MATCH")));
    }
}
