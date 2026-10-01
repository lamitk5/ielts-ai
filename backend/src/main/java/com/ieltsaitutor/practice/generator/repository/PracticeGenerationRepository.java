package com.ieltsaitutor.practice.generator.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeSet;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeVersion;
import com.ieltsaitutor.practice.generator.domain.GenerationState;
import com.ieltsaitutor.practice.generator.domain.GenerationValidationResult;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationBlueprint;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationJob;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.PracticeReviewAction;
import com.ieltsaitutor.rag.domain.RightsStatus;

public interface PracticeGenerationRepository {
    // Sources
    PracticeGenerationSource saveSource(PracticeGenerationSource source);
    Optional<PracticeGenerationSource> findSourceById(UUID id);
    Optional<PracticeGenerationSource> findSourceByChecksum(String checksum);
    List<PracticeGenerationSource> listSources(RightsStatus filterRights);
    void updateSourceRights(UUID sourceId, RightsStatus status, String licenseNote);

    // Blueprints
    PracticeGenerationBlueprint saveBlueprint(PracticeGenerationBlueprint blueprint);
    Optional<PracticeGenerationBlueprint> findBlueprintById(UUID id);
    List<PracticeGenerationBlueprint> listBlueprints(String skill);

    // Jobs
    PracticeGenerationJob saveJob(PracticeGenerationJob job);
    Optional<PracticeGenerationJob> findJobById(UUID id);
    List<PracticeGenerationJob> listJobs(int limit, int offset);
    void updateJobStatus(UUID jobId, String status, String errorMessage);

    // Sets & Versions
    GeneratedPracticeSet saveSet(GeneratedPracticeSet set);
    Optional<GeneratedPracticeSet> findSetById(UUID id);
    List<GeneratedPracticeSet> listSets(GenerationState filterState, int limit, int offset);
    void updateSetState(UUID setId, GenerationState state, UUID currentVersionId);
    void markSetApproved(UUID setId, String publishedSetId, UUID approvedBy);

    GeneratedPracticeVersion saveVersion(GeneratedPracticeVersion version);
    Optional<GeneratedPracticeVersion> findVersionById(UUID id);
    Optional<GeneratedPracticeVersion> findVersionBySetAndNumber(UUID setId, int versionNumber);
    List<GeneratedPracticeVersion> listVersionsForSet(UUID setId);

    // Validation Results
    GenerationValidationResult saveValidationResult(GenerationValidationResult result);
    List<GenerationValidationResult> listValidationResultsForVersion(UUID versionId);

    // Review Actions
    PracticeReviewAction saveReviewAction(PracticeReviewAction action);
    List<PracticeReviewAction> listReviewActionsForSet(UUID setId);
}
