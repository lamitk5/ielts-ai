package com.ieltsaitutor.practice.generator.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.ai.RawQuestionPayload;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;

@Component
public class AnswerKeyValidator implements PracticeValidator {

    public static final String POLICY_VERSION = "answer-key-policy-v1.0";
    private static final Set<String> TFNG_KEYS = Set.of("TRUE", "FALSE", "NOT GIVEN");
    private static final Set<String> YNNG_KEYS = Set.of("YES", "NO", "NOT GIVEN");

    @Override
    public String name() {
        return "AnswerKeyValidator";
    }

    @Override
    public ValidationReport validate(RawPracticePackage pkg, BlueprintSchema blueprint, PracticeGenerationSource source) {
        List<ValidationFinding> findings = new ArrayList<>();

        for (RawQuestionPayload q : pkg.questions()) {
            String key = q.answerKey() != null ? q.answerKey().trim() : "";
            if (key.isBlank()) {
                findings.add(new ValidationFinding("ERR_EMPTY_ANSWER_KEY", ValidationStatus.FAIL, q.id(),
                        "Question answerKey must not be empty", ""));
                continue;
            }

            String taskType = q.taskType() != null ? q.taskType().toUpperCase() : "";

            if (taskType.contains("TRUE_FALSE") || taskType.contains("TFNG")) {
                if (!TFNG_KEYS.contains(key.toUpperCase())) {
                    findings.add(new ValidationFinding("ERR_INVALID_TFNG_KEY", ValidationStatus.FAIL, q.id(),
                            "TFNG question answerKey must be TRUE, FALSE, or NOT GIVEN, found: " + key, ""));
                }
            } else if (taskType.contains("YES_NO") || taskType.contains("YNNG")) {
                if (!YNNG_KEYS.contains(key.toUpperCase())) {
                    findings.add(new ValidationFinding("ERR_INVALID_YNNG_KEY", ValidationStatus.FAIL, q.id(),
                            "YNNG question answerKey must be YES, NO, or NOT GIVEN, found: " + key, ""));
                }
            } else if (taskType.contains("MULTIPLE_CHOICE") || taskType.contains("MCQ")) {
                boolean matchesLetter = key.matches("^[A-Da-d]$");
                boolean matchesOption = q.options().stream().anyMatch(opt -> opt.equalsIgnoreCase(key));
                if (!matchesLetter && !matchesOption) {
                    findings.add(new ValidationFinding("ERR_INVALID_MCQ_KEY", ValidationStatus.FAIL, q.id(),
                            "Multiple choice answerKey must be a valid option index (A-D) or match an option text, found: " + key, ""));
                }
            } else if (taskType.contains("COMPLETION") || taskType.contains("SHORT_ANSWER")) {
                String[] words = key.split("\\s+");
                int maxWords = (blueprint != null && blueprint.reasoningRules() != null)
                        ? blueprint.reasoningRules().maxAnswerLengthWords()
                        : 3;
                if (words.length > maxWords) {
                    findings.add(new ValidationFinding("ERR_KEY_EXCEEDS_WORD_LIMIT", ValidationStatus.FAIL, q.id(),
                            String.format("Completion answer key has %d words, exceeding limit of %d words: '%s'", words.length, maxWords, key),
                            ""));
                }
            }
        }

        boolean hasFail = findings.stream().anyMatch(f -> f.severity() == ValidationStatus.FAIL);
        boolean hasWarn = findings.stream().anyMatch(f -> f.severity() == ValidationStatus.WARNING);
        ValidationStatus overall = hasFail ? ValidationStatus.FAIL : (hasWarn ? ValidationStatus.WARNING : ValidationStatus.PASS);

        return new ValidationReport(name(), POLICY_VERSION, overall, findings);
    }
}
