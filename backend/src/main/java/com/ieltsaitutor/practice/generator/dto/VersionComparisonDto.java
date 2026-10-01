package com.ieltsaitutor.practice.generator.dto;

import java.util.List;
import java.util.UUID;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeVersion;
import com.ieltsaitutor.practice.generator.domain.GenerationValidationResult;

public record VersionComparisonDto(
    UUID setId,
    GeneratedPracticeVersion baseVersion,
    GeneratedPracticeVersion compareVersion,
    boolean passageChanged,
    int baseQuestionCount,
    int compareQuestionCount,
    List<String> questionDifferences,
    List<GenerationValidationResult> baseValidationResults,
    List<GenerationValidationResult> compareValidationResults
) {}
