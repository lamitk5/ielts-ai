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

class AmbiguityValidatorTest {

    private AmbiguityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AmbiguityValidator();
    }

    @Test
    void passesDistinctOptions() {
        RawPracticePackage pkg = new RawPracticePackage(
                "Title",
                new RawPassagePayload("Title", List.of(new RawPassagePayload.RawParagraphPayload("p1", "Text"))),
                List.of(
                        new RawQuestionPayload("q1", "MULTIPLE_CHOICE", "What is the primary factor?",
                                List.of("Temperature", "Pressure", "Salinity", "Density"), "A", "Span", "Exp")
                ),
                "model", "v1", Instant.now()
        );

        ValidationReport report = validator.validate(pkg, null, null);
        assertEquals(ValidationStatus.PASS, report.status());
    }

    @Test
    void failsOnDuplicateOptions() {
        RawPracticePackage pkg = new RawPracticePackage(
                "Title",
                new RawPassagePayload("Title", List.of(new RawPassagePayload.RawParagraphPayload("p1", "Text"))),
                List.of(
                        new RawQuestionPayload("q1", "MULTIPLE_CHOICE", "What is the primary factor?",
                                List.of("Temperature", "Pressure", "Temperature", "Density"), "A", "Span", "Exp")
                ),
                "model", "v1", Instant.now()
        );

        ValidationReport report = validator.validate(pkg, null, null);
        assertEquals(ValidationStatus.FAIL, report.status());
        assertTrue(report.findings().stream().anyMatch(f -> f.code().equals("ERR_DUPLICATE_OPTION")));
    }
}
