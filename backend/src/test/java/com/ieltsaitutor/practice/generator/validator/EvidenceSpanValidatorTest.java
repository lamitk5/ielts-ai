package com.ieltsaitutor.practice.generator.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.generator.ai.RawPassagePayload;
import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.ai.RawQuestionPayload;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;

class EvidenceSpanValidatorTest {

    private EvidenceSpanValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EvidenceSpanValidator();
    }

    @Test
    void passesWhenEvidenceSpanExistsVerbatimInPassage() {
        RawPracticePackage pkg = new RawPracticePackage(
                "Marine Ecology",
                new RawPassagePayload("Marine Ecology", List.of(
                        new RawPassagePayload.RawParagraphPayload("p1", "Coral reef systems demonstrate resilience under thermal stress.")
                )),
                List.of(
                        new RawQuestionPayload("q1", "TRUE_FALSE_NOT_GIVEN", "Prompt",
                                List.of("TRUE", "FALSE", "NOT GIVEN"), "TRUE",
                                "Coral reef systems demonstrate resilience under thermal stress", "Exp")
                ),
                "model", "v1", Instant.now()
        );

        ValidationReport report = validator.validate(pkg, null, null);
        assertEquals(ValidationStatus.PASS, report.status());
    }

    @Test
    void failsWhenEvidenceSpanDoesNotExistInPassage() {
        RawPracticePackage pkg = new RawPracticePackage(
                "Marine Ecology",
                new RawPassagePayload("Marine Ecology", List.of(
                        new RawPassagePayload.RawParagraphPayload("p1", "Coral reef systems demonstrate resilience under thermal stress.")
                )),
                List.of(
                        new RawQuestionPayload("q1", "TRUE_FALSE_NOT_GIVEN", "Prompt",
                                List.of("TRUE", "FALSE", "NOT GIVEN"), "TRUE",
                                "Completely fabricated evidence span not in the passage", "Exp")
                ),
                "model", "v1", Instant.now()
        );

        ValidationReport report = validator.validate(pkg, null, null);
        assertEquals(ValidationStatus.FAIL, report.status());
        assertTrue(report.findings().stream().anyMatch(f -> f.code().equals("ERR_EVIDENCE_SPAN_NOT_VERBATIM")));
    }
}
