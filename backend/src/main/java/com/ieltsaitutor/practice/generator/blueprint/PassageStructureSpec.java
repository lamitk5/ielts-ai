package com.ieltsaitutor.practice.generator.blueprint;

public record PassageStructureSpec(
        int targetWordCount,
        int paragraphCount,
        String rhetoricalPattern,
        String lexicalDensity) {

    public PassageStructureSpec {
        if (targetWordCount <= 0) targetWordCount = 750;
        if (paragraphCount <= 0) paragraphCount = 6;
        if (rhetoricalPattern == null || rhetoricalPattern.isBlank()) rhetoricalPattern = "EXPOSITORY_AND_ANALYTICAL";
        if (lexicalDensity == null || lexicalDensity.isBlank()) lexicalDensity = "ACADEMIC_CEFR_C1";
    }
}
