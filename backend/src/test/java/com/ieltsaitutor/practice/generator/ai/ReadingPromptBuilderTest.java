package com.ieltsaitutor.practice.generator.ai;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.blueprint.ItemDistributionSpec;
import com.ieltsaitutor.practice.generator.blueprint.PassageStructureSpec;
import com.ieltsaitutor.practice.generator.blueprint.ReasoningRulesSpec;
import com.ieltsaitutor.rag.domain.Skill;

class ReadingPromptBuilderTest {

    @Test
    void buildsStructuredPromptFromBlueprint() {
        ReadingPromptBuilder builder = new ReadingPromptBuilder();
        BlueprintSchema blueprint = new BlueprintSchema(
                "bp-1",
                Skill.READING,
                BigDecimal.valueOf(7.5),
                "OCEAN_MICROBIOLOGY",
                new PassageStructureSpec(800, 6, "EXPOSITORY", "ACADEMIC_CEFR_C1"),
                List.of(
                        new ItemDistributionSpec("TRUE_FALSE_NOT_GIVEN", 4, List.of(1, 2), "FACTUAL_DETAIL"),
                        new ItemDistributionSpec("MULTIPLE_CHOICE", 3, List.of(3, 4), "INFERENCE")
                ),
                new ReasoningRulesSpec(true, true, 3)
        );

        String prompt = builder.buildPrompt(blueprint, "Arctic Estuarine Microplastics");
        assertNotNull(prompt);
        assertTrue(prompt.contains("Target IELTS Band: 7.5"));
        assertTrue(prompt.contains("Arctic Estuarine Microplastics"));
        assertTrue(prompt.contains("TRUE_FALSE_NOT_GIVEN"));
        assertTrue(prompt.contains("MULTIPLE_CHOICE"));
        assertTrue(prompt.contains("evidenceSpan"));
        assertTrue(prompt.contains("answerKey"));
    }
}
