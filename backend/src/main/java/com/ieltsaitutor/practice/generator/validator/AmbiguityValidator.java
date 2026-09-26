package com.ieltsaitutor.practice.generator.validator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.ai.RawQuestionPayload;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;

@Component
public class AmbiguityValidator implements PracticeValidator {

    public static final String POLICY_VERSION = "ambiguity-policy-v1.0";

    @Override
    public String name() {
        return "AmbiguityValidator";
    }

    @Override
    public ValidationReport validate(RawPracticePackage pkg, BlueprintSchema blueprint, PracticeGenerationSource source) {
        List<ValidationFinding> findings = new ArrayList<>();

        for (RawQuestionPayload q : pkg.questions()) {
            if (q.options() != null && !q.options().isEmpty()) {
                Set<String> normalizedOptions = new HashSet<>();
                for (String opt : q.options()) {
                    String norm = opt.trim().toLowerCase();
                    if (!norm.isBlank() && !normalizedOptions.add(norm)) {
                        findings.add(new ValidationFinding("ERR_DUPLICATE_OPTION", ValidationStatus.FAIL, q.id(),
                                String.format("Duplicate or identical distractor option found in question: '%s'", opt),
                                "Distractor uniqueness check failed"));
                    }
                }
            }

            // Check if prompt accidentally contains the exact answer key option (for non-TFNG)
            String promptNorm = q.prompt() != null ? q.prompt().toLowerCase() : "";
            String keyNorm = q.answerKey() != null ? q.answerKey().trim().toLowerCase() : "";
            if (!keyNorm.isBlank() && keyNorm.length() > 3
                    && !keyNorm.equalsIgnoreCase("true")
                    && !keyNorm.equalsIgnoreCase("false")
                    && !keyNorm.equalsIgnoreCase("not given")
                    && promptNorm.contains(keyNorm) && !promptNorm.contains("blank") && !promptNorm.contains("_____")) {
                findings.add(new ValidationFinding("WARN_PROMPT_CONTAINS_KEY", ValidationStatus.WARNING, q.id(),
                        String.format("Question prompt may reveal the answer key: '%s'", q.answerKey()),
                        "Prompt-stem leakage advisory"));
            }
        }

        boolean hasFail = findings.stream().anyMatch(f -> f.severity() == ValidationStatus.FAIL);
        boolean hasWarn = findings.stream().anyMatch(f -> f.severity() == ValidationStatus.WARNING);
        ValidationStatus overall = hasFail ? ValidationStatus.FAIL : (hasWarn ? ValidationStatus.WARNING : ValidationStatus.PASS);

        return new ValidationReport(name(), POLICY_VERSION, overall, findings);
    }
}
