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

class AnswerKeyValidatorTest {

    private AnswerKeyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AnswerKeyValidator();
    }

    @Test
    void validatesValidTfngAndMcqKeys() {
        RawPracticePackage pkg = new RawPracticePackage(
                "Title",
                new RawPassagePayload("Title", List.of(new RawPassagePayload.RawParagraphPayload("p1", "Text"))),
                List.of(
                        new RawQuestionPayload("q1", "TRUE_FALSE_NOT_GIVEN", "Prompt", List.of("TRUE", "FALSE", "NOT GIVEN"), "TRUE", "Span", "Exp"),
                        new RawQuestionPayload("q2", "MULTIPLE_CHOICE", "Prompt", List.of("Option A", "Option B", "Option C", "Option D"), "A", "Span", "Exp")
                ),
                "model", "v1", Instant.now()
        );

        ValidationReport report = validator.validate(pkg, null, null);
        assertEquals(ValidationStatus.PASS, report.status());
    }

    @Test
    void rejectsInvalidAnswerKeys() {
        RawPracticePackage pkg = new RawPracticePackage(
                "Title",
                new RawPassagePayload("Title", List.of(new RawPassagePayload.RawParagraphPayload("p1", "Text"))),
                List.of(
                        new RawQuestionPayload("q1", "TRUE_FALSE_NOT_GIVEN", "Prompt", List.of("TRUE", "FALSE", "NOT GIVEN"), "MAYBE", "Span", "Exp"),
                        new RawQuestionPayload("q2", "MULTIPLE_CHOICE", "Prompt", List.of("Option A", "Option B", "Option C", "Option D"), "Z", "Span", "Exp")
                ),
                "model", "v1", Instant.now()
        );

        ValidationReport report = validator.validate(pkg, null, null);
        assertEquals(ValidationStatus.FAIL, report.status());
        assertTrue(report.findings().stream().anyMatch(f -> f.code().equals("ERR_INVALID_TFNG_KEY")));
        assertTrue(report.findings().stream().anyMatch(f -> f.code().equals("ERR_INVALID_MCQ_KEY")));
    }
}
