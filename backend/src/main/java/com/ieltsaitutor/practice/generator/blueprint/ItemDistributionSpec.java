package com.ieltsaitutor.practice.generator.blueprint;

import java.util.List;

public record ItemDistributionSpec(
        String taskType,
        int count,
        List<Integer> targetParagraphs,
        String cognitiveSkill) {

    public ItemDistributionSpec {
        if (targetParagraphs == null) targetParagraphs = List.of();
        if (cognitiveSkill == null) cognitiveSkill = "FACTUAL_DETAIL_AND_INFERENCE";
    }
}
