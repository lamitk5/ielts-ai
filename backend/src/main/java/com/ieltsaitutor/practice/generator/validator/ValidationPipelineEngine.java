package com.ieltsaitutor.practice.generator.validator;

import java.util.List;

import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;

public interface ValidationPipelineEngine {
    AggregateValidationDecision validate(RawPracticePackage pkg, BlueprintSchema blueprint, PracticeGenerationSource source);

    record AggregateValidationDecision(
            ValidationStatus status,
            List<ValidationReport> reports,
            String summary) {}
}
