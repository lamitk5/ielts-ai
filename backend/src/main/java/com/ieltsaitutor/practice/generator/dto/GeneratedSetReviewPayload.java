package com.ieltsaitutor.practice.generator.dto;

import java.util.List;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeSet;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeVersion;
import com.ieltsaitutor.practice.generator.domain.GenerationValidationResult;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationBlueprint;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.PracticeReviewAction;

public record GeneratedSetReviewPayload(
    GeneratedPracticeSet practiceSet,
    GeneratedPracticeVersion currentVersion,
    PracticeGenerationBlueprint blueprint,
    PracticeGenerationSource source,
    List<GenerationValidationResult> validationResults,
    List<PracticeReviewAction> reviewHistory,
    List<GeneratedPracticeVersion> versionHistory
) {}
