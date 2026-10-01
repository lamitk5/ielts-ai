package com.ieltsaitutor.practice.generator.blueprint;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ieltsaitutor.rag.domain.Skill;

@Component
public class DefaultBlueprintCatalog {

    public BlueprintSchema readingPassage1() {
        return new BlueprintSchema(
                "bp-reading-p1-standard",
                Skill.READING,
                BigDecimal.valueOf(6.5),
                "ENVIRONMENTAL_AND_EARTH_SCIENCES",
                new PassageStructureSpec(700, 5, "DESCRIPTIVE_AND_EXPOSITORY", "ACADEMIC_CEFR_B2_C1"),
                List.of(
                        new ItemDistributionSpec("TRUE_FALSE_NOT_GIVEN", 5, List.of(1, 2, 3), "FACTUAL_DETAIL_VERIFICATION"),
                        new ItemDistributionSpec("SUMMARY_COMPLETION", 5, List.of(4, 5), "SYNTACTIC_AND_LEXICAL_SYNONYMY")
                ),
                new ReasoningRulesSpec(true, true, 2)
        );
    }

    public BlueprintSchema readingPassage2() {
        return new BlueprintSchema(
                "bp-reading-p2-academic",
                Skill.READING,
                BigDecimal.valueOf(7.5),
                "TECHNOLOGY_AND_APPLIED_SCIENCES",
                new PassageStructureSpec(800, 6, "CHRONOLOGICAL_DISCOVERY_AND_EVALUATION", "ACADEMIC_CEFR_C1"),
                List.of(
                        new ItemDistributionSpec("MATCHING_HEADINGS", 4, List.of(1, 2, 3, 4), "PARAGRAPH_MAIN_IDEA"),
                        new ItemDistributionSpec("MULTIPLE_CHOICE", 3, List.of(4, 5), "INFERENCE_AND_PURPOSE"),
                        new ItemDistributionSpec("TRUE_FALSE_NOT_GIVEN", 3, List.of(5, 6), "FACTUAL_DETAIL_VERIFICATION")
                ),
                new ReasoningRulesSpec(true, true, 3)
        );
    }

    public BlueprintSchema readingPassage3() {
        return new BlueprintSchema(
                "bp-reading-p3-advanced",
                Skill.READING,
                BigDecimal.valueOf(8.5),
                "HUMANITIES_AND_SOCIAL_THEORY",
                new PassageStructureSpec(900, 7, "ARGUMENTATIVE_AND_DISCURSIVE", "ACADEMIC_CEFR_C1_C2"),
                List.of(
                        new ItemDistributionSpec("MULTIPLE_CHOICE", 4, List.of(1, 2, 3), "COMPLEX_INFERENCE_AND_TONE"),
                        new ItemDistributionSpec("YES_NO_NOT_GIVEN", 3, List.of(4, 5), "WRITER_VIEWS_AND_CLAIMS"),
                        new ItemDistributionSpec("SUMMARY_COMPLETION", 3, List.of(6, 7), "CONCEPTUAL_SUMMARY_COMPLETION")
                ),
                new ReasoningRulesSpec(true, true, 3)
        );
    }

    public List<BlueprintSchema> listAll() {
        return List.of(readingPassage1(), readingPassage2(), readingPassage3());
    }
}
