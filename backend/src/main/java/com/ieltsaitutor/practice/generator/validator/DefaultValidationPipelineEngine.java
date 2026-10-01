package com.ieltsaitutor.practice.generator.validator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.critic.AiCriticService;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;

@Service
public class DefaultValidationPipelineEngine implements ValidationPipelineEngine {

    private final List<PracticeValidator> validators;
    private final AiCriticService aiCriticService;

    public DefaultValidationPipelineEngine(List<PracticeValidator> validators, AiCriticService aiCriticService) {
        this.validators = validators;
        this.aiCriticService = aiCriticService;
    }

    @Override
    public AggregateValidationDecision validate(RawPracticePackage pkg, BlueprintSchema blueprint, PracticeGenerationSource source) {
        List<ValidationReport> reports = new ArrayList<>();

        boolean hasFail = false;
        boolean hasWarn = false;

        // 1. Run all deterministic and heuristic validators
        for (PracticeValidator validator : validators) {
            ValidationReport report = validator.validate(pkg, blueprint, source);
            reports.add(report);
            if (report.status() == ValidationStatus.FAIL) {
                hasFail = true;
            } else if (report.status() == ValidationStatus.WARNING) {
                hasWarn = true;
            }
        }

        // 2. Run Advisory AI Critic (advisory only)
        if (aiCriticService != null) {
            ValidationReport criticReport = aiCriticService.evaluate(pkg);
            reports.add(criticReport);
        }

        // 3. Aggregate Decision (Deterministic failure takes strict precedence)
        ValidationStatus aggregateStatus = hasFail
                ? ValidationStatus.FAIL
                : (hasWarn ? ValidationStatus.WARNING : ValidationStatus.PASS);

        String summary = String.format("Validation completed across %d gates with status: %s (Deterministic Failures: %s, Warnings: %s)",
                reports.size(), aggregateStatus, hasFail, hasWarn);

        return new AggregateValidationDecision(aggregateStatus, reports, summary);
    }
}
