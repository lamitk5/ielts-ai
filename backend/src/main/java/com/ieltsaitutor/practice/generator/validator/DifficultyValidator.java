package com.ieltsaitutor.practice.generator.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ieltsaitutor.practice.generator.ai.RawPassagePayload;
import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;

@Component
public class DifficultyValidator implements PracticeValidator {

    public static final String POLICY_VERSION = "difficulty-heuristic-v1.0";

    private final ReadabilityMetricsCalculator readabilityCalculator;
    private final AcademicWordListIndex awlIndex;

    public DifficultyValidator(ReadabilityMetricsCalculator readabilityCalculator, AcademicWordListIndex awlIndex) {
        this.readabilityCalculator = readabilityCalculator;
        this.awlIndex = awlIndex;
    }

    @Override
    public String name() {
        return "DifficultyValidator";
    }

    @Override
    public ValidationReport validate(RawPracticePackage pkg, BlueprintSchema blueprint, PracticeGenerationSource source) {
        List<ValidationFinding> findings = new ArrayList<>();

        String fullText = pkg.passage().paragraphs().stream()
                .map(RawPassagePayload.RawParagraphPayload::text)
                .collect(Collectors.joining(" "));

        ReadabilityMetricsCalculator.ReadabilityResult readability = readabilityCalculator.calculate(fullText);
        double awlDensity = awlIndex.calculateAwlDensity(fullText);
        double targetBand = blueprint != null && blueprint.targetBand() != null
                ? blueprint.targetBand().doubleValue()
                : 7.0;

        // Band 7.0+ heuristic range: FK Grade 9.0 - 15.0, AWL Density >= 4%
        // Band 8.0+ heuristic range: FK Grade 11.0 - 16.0, AWL Density >= 6%
        double minFk = targetBand >= 8.0 ? 10.0 : (targetBand >= 7.0 ? 8.5 : 7.0);
        double maxFk = targetBand >= 8.0 ? 16.5 : (targetBand >= 7.0 ? 15.5 : 14.0);

        if (readability.fleschKincaidGradeLevel() < minFk || readability.fleschKincaidGradeLevel() > maxFk) {
            findings.add(new ValidationFinding(
                    "WARN_READABILITY_PROXY_OUT_OF_RANGE",
                    ValidationStatus.WARNING,
                    "passage",
                    String.format("Calculated Flesch-Kincaid grade level %.1f is outside estimated proxy range [%.1f - %.1f] for Target Band %.1f",
                            readability.fleschKincaidGradeLevel(), minFk, maxFk, targetBand),
                    "Heuristic readability proxy alignment only; does not replace human expert review"
            ));
        }

        if (awlDensity < 0.03 && targetBand >= 7.0) {
            findings.add(new ValidationFinding(
                    "WARN_LOW_ACADEMIC_DENSITY",
                    ValidationStatus.WARNING,
                    "passage",
                    String.format("Academic Word List density %.1f%% is lower than typical target for Band %.1f",
                            awlDensity * 100.0, targetBand),
                    "Heuristic lexical density proxy check"
            ));
        }

        boolean hasFail = findings.stream().anyMatch(f -> f.severity() == ValidationStatus.FAIL);
        boolean hasWarn = findings.stream().anyMatch(f -> f.severity() == ValidationStatus.WARNING);
        ValidationStatus overall = hasFail ? ValidationStatus.FAIL : (hasWarn ? ValidationStatus.WARNING : ValidationStatus.PASS);

        return new ValidationReport(name(), POLICY_VERSION, overall, findings);
    }
}
