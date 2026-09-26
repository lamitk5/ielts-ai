package com.ieltsaitutor.practice.generator.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.rag.domain.Skill;

class BlueprintSchemaTest {

    @Test
    void constructsValidDefaultSchema() {
        BlueprintSchema schema = new BlueprintSchema(
                "bp-test",
                Skill.READING,
                BigDecimal.valueOf(7.5),
                "ENVIRONMENTAL_SCIENCE",
                new PassageStructureSpec(800, 6, "CHRONOLOGICAL_DISCOVERY", "ACADEMIC_CEFR_C1"),
                List.of(new ItemDistributionSpec("TRUE_FALSE_NOT_GIVEN", 4, List.of(1, 2), "FACTUAL_VERIFICATION")),
                new ReasoningRulesSpec(true, true, 3)
        );

        assertNotNull(schema);
        assertEquals("bp-test", schema.blueprintId());
        assertEquals(Skill.READING, schema.skill());
        assertEquals(BigDecimal.valueOf(7.5), schema.targetBand());
        assertEquals(1, schema.itemDistribution().size());
        assertTrue(schema.reasoningRules().requireVerbatimEvidenceSpan());
    }
}
