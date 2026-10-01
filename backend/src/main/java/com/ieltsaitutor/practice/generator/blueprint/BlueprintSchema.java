package com.ieltsaitutor.practice.generator.blueprint;

import java.math.BigDecimal;
import java.util.List;

import com.ieltsaitutor.rag.domain.Skill;

public record BlueprintSchema(
        String blueprintId,
        Skill skill,
        BigDecimal targetBand,
        String topicCategory,
        PassageStructureSpec passageStructure,
        List<ItemDistributionSpec> itemDistribution,
        ReasoningRulesSpec reasoningRules) {

    public BlueprintSchema {
        if (skill == null) skill = Skill.READING;
        if (targetBand == null) targetBand = BigDecimal.valueOf(7.0);
        if (topicCategory == null || topicCategory.isBlank()) topicCategory = "GENERAL_ACADEMIC";
        if (passageStructure == null) passageStructure = new PassageStructureSpec(750, 6, "EXPOSITORY_AND_ANALYTICAL", "ACADEMIC_CEFR_C1");
        if (itemDistribution == null) itemDistribution = List.of();
        if (reasoningRules == null) reasoningRules = new ReasoningRulesSpec(true, true, 3);
    }
}
