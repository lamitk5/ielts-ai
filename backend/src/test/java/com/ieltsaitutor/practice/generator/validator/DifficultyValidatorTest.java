package com.ieltsaitutor.practice.generator.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.generator.ai.RawPassagePayload;
import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;
import com.ieltsaitutor.rag.domain.Skill;

class DifficultyValidatorTest {

    private DifficultyValidator validator;

    @BeforeEach
    void setUp() {
        ReadabilityMetricsCalculator calc = new ReadabilityMetricsCalculator();
        AcademicWordListIndex awl = new AcademicWordListIndex();
        validator = new DifficultyValidator(calc, awl);
    }

    @Test
    void validatesAcademicPassageReadabilityAndAwlDensity() {
        String academicPassage = """
                Recent investigations into marine biogeochemical dynamics indicate significant shifts in oceanic pH levels.
                Researchers conducted extensive empirical analyses across multiple sub-polar estuaries to determine the distribution
                of synthetic chemical compounds and particulate sedimentation. The data demonstrates a strong correlation between
                temperature fluctuations and altered microbial metabolic rates. Consequently, ecological authorities recommend comprehensive
                monitoring protocols to evaluate environmental resilience.
                """;

        RawPracticePackage pkg = new RawPracticePackage(
                "Marine Biogeochemistry",
                new RawPassagePayload("Marine Biogeochemistry", List.of(
                        new RawPassagePayload.RawParagraphPayload("p1", academicPassage)
                )),
                List.of(),
                "model", "v1", Instant.now()
        );

        BlueprintSchema blueprint = new BlueprintSchema(
                "bp-1", Skill.READING, BigDecimal.valueOf(7.5), "ENVIRONMENTAL_SCIENCE",
                null, List.of(), null);

        ValidationReport report = validator.validate(pkg, blueprint, null);
        assertNotNull(report);
        assertEquals("DifficultyValidator", report.validatorName());
        assertEquals("difficulty-heuristic-v1.0", report.policyVersion());
    }
}
