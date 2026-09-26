package com.ieltsaitutor.practice.generator.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.generator.ai.RawPassagePayload;
import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.critic.AiCriticService;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;

class ValidationPipelineEngineTest {

    private PracticeValidator passingValidator;
    private PracticeValidator failingValidator;
    private AiCriticService criticService;

    @BeforeEach
    void setUp() {
        passingValidator = mock(PracticeValidator.class);
        when(passingValidator.validate(any(), any(), any()))
                .thenReturn(new ValidationReport("PassVal", "v1", ValidationStatus.PASS, List.of()));

        failingValidator = mock(PracticeValidator.class);
        when(failingValidator.validate(any(), any(), any()))
                .thenReturn(new ValidationReport("FailVal", "v1", ValidationStatus.FAIL,
                        List.of(new ValidationFinding("ERR_DETERMINISTIC", ValidationStatus.FAIL, "q1", "Key invalid", ""))));

        criticService = mock(AiCriticService.class);
        when(criticService.evaluate(any()))
                .thenReturn(new ValidationReport("AiCritic", "v1", ValidationStatus.PASS,
                        List.of(new ValidationFinding("INFO_ADVISORY", ValidationStatus.PASS, "all", "Great!", ""))));
    }

    @Test
    void deterministicFailureCannotBeOverriddenByAiCritic() {
        DefaultValidationPipelineEngine engine = new DefaultValidationPipelineEngine(
                List.of(passingValidator, failingValidator), criticService);

        RawPracticePackage pkg = new RawPracticePackage(
                "Title", new RawPassagePayload("Title", List.of()), List.of(), "model", "v1", Instant.now());

        ValidationPipelineEngine.AggregateValidationDecision decision = engine.validate(pkg, null, null);
        assertEquals(ValidationStatus.FAIL, decision.status());
    }

    @Test
    void allPassingYieldsPassDecision() {
        DefaultValidationPipelineEngine engine = new DefaultValidationPipelineEngine(
                List.of(passingValidator), criticService);

        RawPracticePackage pkg = new RawPracticePackage(
                "Title", new RawPassagePayload("Title", List.of()), List.of(), "model", "v1", Instant.now());

        ValidationPipelineEngine.AggregateValidationDecision decision = engine.validate(pkg, null, null);
        assertEquals(ValidationStatus.PASS, decision.status());
    }
}
