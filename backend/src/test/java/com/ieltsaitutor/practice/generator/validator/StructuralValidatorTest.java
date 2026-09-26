package com.ieltsaitutor.practice.generator.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.generator.ai.RawPassagePayload;
import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.ai.RawQuestionPayload;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;
import com.ieltsaitutor.rag.domain.Skill;

class StructuralValidatorTest {

    private StructuralValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StructuralValidator();
    }

    @Test
    void passesCompleteStructuralPackage() {
        RawPracticePackage pkg = new RawPracticePackage(
                "Passage Title",
                new RawPassagePayload("Passage Title", List.of(
                        new RawPassagePayload.RawParagraphPayload("p1", "Text of paragraph 1"),
                        new RawPassagePayload.RawParagraphPayload("p2", "Text of paragraph 2")
                )),
                List.of(
                        new RawQuestionPayload("q1", "MULTIPLE_CHOICE", "Prompt 1", List.of("A", "B", "C", "D"), "A", "Span 1", "Exp 1"),
                        new RawQuestionPayload("q2", "TRUE_FALSE_NOT_GIVEN", "Prompt 2", List.of("TRUE", "FALSE", "NOT GIVEN"), "TRUE", "Span 2", "Exp 2")
                ),
                "model-1", "v1.0", Instant.now()
        );

        ValidationReport report = validator.validate(pkg, null, null);
        assertEquals(ValidationStatus.PASS, report.status());
        assertTrue(report.findings().isEmpty());
    }

    @Test
    void failsWhenPassageOrParagraphsMissing() {
        RawPracticePackage pkg = new RawPracticePackage(
                "Passage Title",
                new RawPassagePayload("Passage Title", List.of()),
                List.of(new RawQuestionPayload("q1", "MCQ", "Prompt", List.of("A", "B", "C", "D"), "A", "Span", "Exp")),
                "model-1", "v1.0", Instant.now()
        );

        ValidationReport report = validator.validate(pkg, null, null);
        assertEquals(ValidationStatus.FAIL, report.status());
        assertTrue(report.findings().stream().anyMatch(f -> f.code().equals("ERR_EMPTY_PASSAGE")));
    }
}
