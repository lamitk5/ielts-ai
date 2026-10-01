package com.ieltsaitutor.practice.generator.validator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.ai.RawQuestionPayload;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.blueprint.ItemDistributionSpec;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;

@Component
public class StructuralValidator implements PracticeValidator {

    public static final String POLICY_VERSION = "structural-policy-v1.0";

    @Override
    public String name() {
        return "StructuralValidator";
    }

    @Override
    public ValidationReport validate(RawPracticePackage pkg, BlueprintSchema blueprint, PracticeGenerationSource source) {
        List<ValidationFinding> findings = new ArrayList<>();

        if (pkg.passage() == null || pkg.passage().paragraphs().isEmpty()) {
            findings.add(new ValidationFinding("ERR_EMPTY_PASSAGE", ValidationStatus.FAIL, "passage",
                    "Generated passage must contain at least one paragraph", ""));
        } else {
            for (var p : pkg.passage().paragraphs()) {
                if (p.text() == null || p.text().isBlank()) {
                    findings.add(new ValidationFinding("ERR_EMPTY_PARAGRAPH", ValidationStatus.FAIL, p.id(),
                            "Paragraph text cannot be empty", ""));
                }
            }
        }

        if (pkg.questions().isEmpty()) {
            findings.add(new ValidationFinding("ERR_NO_QUESTIONS", ValidationStatus.FAIL, "questions",
                    "Generated package must contain questions", ""));
        }

        if (blueprint != null && !blueprint.itemDistribution().isEmpty()) {
            int expectedCount = blueprint.itemDistribution().stream().mapToInt(ItemDistributionSpec::count).sum();
            int actualCount = pkg.questions().size();
            if (actualCount != expectedCount) {
                findings.add(new ValidationFinding("WARN_ITEM_COUNT_MISMATCH", ValidationStatus.WARNING, "questions",
                        String.format("Expected %d questions from blueprint, but generated %d", expectedCount, actualCount),
                        ""));
            }
        }

        for (RawQuestionPayload q : pkg.questions()) {
            if (q.id() == null || q.id().isBlank()) {
                findings.add(new ValidationFinding("ERR_MISSING_QUESTION_ID", ValidationStatus.FAIL, "question",
                        "Question missing ID", ""));
            }
            if (q.prompt() == null || q.prompt().isBlank()) {
                findings.add(new ValidationFinding("ERR_EMPTY_PROMPT", ValidationStatus.FAIL, q.id(),
                        "Question prompt cannot be empty", ""));
            }
            if ("MULTIPLE_CHOICE".equalsIgnoreCase(q.taskType()) && q.options().size() < 3) {
                findings.add(new ValidationFinding("ERR_INSUFFICIENT_OPTIONS", ValidationStatus.FAIL, q.id(),
                        "Multiple choice question must have at least 3 options, found: " + q.options().size(), ""));
            }
        }

        boolean hasFail = findings.stream().anyMatch(f -> f.severity() == ValidationStatus.FAIL);
        boolean hasWarn = findings.stream().anyMatch(f -> f.severity() == ValidationStatus.WARNING);
        ValidationStatus overall = hasFail ? ValidationStatus.FAIL : (hasWarn ? ValidationStatus.WARNING : ValidationStatus.PASS);

        return new ValidationReport(name(), POLICY_VERSION, overall, findings);
    }
}
