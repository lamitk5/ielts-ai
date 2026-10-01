package com.ieltsaitutor.practice.generator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.generator.ai.RawPassagePayload;
import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.ai.RawQuestionPayload;
import com.ieltsaitutor.practice.generator.ai.ReadingPracticeGeneratorService;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintExtractorService;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.domain.GenerationState;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationBlueprint;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;
import com.ieltsaitutor.practice.generator.dto.CreateJobRequest;
import com.ieltsaitutor.practice.generator.dto.JobStatusResponse;
import com.ieltsaitutor.practice.generator.exception.SourceRightsException;
import com.ieltsaitutor.practice.generator.repository.PracticeGenerationRepository;
import com.ieltsaitutor.practice.generator.validator.ValidationPipelineEngine;
import com.ieltsaitutor.practice.generator.validator.ValidationReport;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.domain.Skill;

class GenerationJobOrchestratorTest {

    private PracticeGenerationRepository repository;
    private SourceRightsGuard rightsGuard;
    private BlueprintExtractorService blueprintExtractorService;
    private ReadingPracticeGeneratorService generatorService;
    private GenerationStateMachine stateMachine;
    private ValidationPipelineEngine validationEngine;
    private GenerationJobOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        repository = mock(PracticeGenerationRepository.class);
        rightsGuard = mock(SourceRightsGuard.class);
        blueprintExtractorService = mock(BlueprintExtractorService.class);
        generatorService = mock(ReadingPracticeGeneratorService.class);
        stateMachine = new GenerationStateMachine();
        validationEngine = mock(ValidationPipelineEngine.class);

        orchestrator = new GenerationJobOrchestrator(
                repository, rightsGuard, blueprintExtractorService,
                generatorService, stateMachine, validationEngine
        );
    }

    @Test
    void runsCompleteHappyPathGenerationJobToPendingReview() {
        UUID sourceId = UUID.randomUUID();
        UUID bpId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        PracticeGenerationSource source = new PracticeGenerationSource(
                sourceId, "Coral Ecology", "PASTED_TEXT", "Author",
                RightsStatus.APPROVED, "License", "Coral reefs content...", "hash", adminId,
                Instant.now(), Instant.now());
        when(rightsGuard.verifySourceCanGenerate(sourceId)).thenReturn(source);

        PracticeGenerationBlueprint bpEntity = new PracticeGenerationBlueprint(
                bpId, Skill.READING, "BP 1", BigDecimal.valueOf(7.0), "{}", adminId, Instant.now(), Instant.now());
        when(repository.findBlueprintById(bpId)).thenReturn(Optional.of(bpEntity));

        BlueprintSchema schema = new BlueprintSchema("bp-1", Skill.READING, BigDecimal.valueOf(7.0), "MARINE_ECOLOGY", null, List.of(), null);
        when(blueprintExtractorService.fromJson("{}")).thenReturn(schema);

        RawPracticePackage pkg = new RawPracticePackage(
                "Coral Ecology",
                new RawPassagePayload("Coral Ecology", List.of(new RawPassagePayload.RawParagraphPayload("p1", "Text..."))),
                List.of(new RawQuestionPayload("q1", "MCQ", "Prompt", List.of("A"), "A", "Span", "Exp")),
                "gemini-1.5-pro", "v1.0", Instant.now());
        when(generatorService.generate(any(), any())).thenReturn(pkg);

        ValidationPipelineEngine.AggregateValidationDecision decision = new ValidationPipelineEngine.AggregateValidationDecision(
                ValidationStatus.PASS,
                List.of(new ValidationReport("StructuralValidator", "v1.0", ValidationStatus.PASS, List.of())),
                "All checks passed"
        );
        when(validationEngine.validate(any(), any(), any())).thenReturn(decision);

        CreateJobRequest request = new CreateJobRequest(sourceId, bpId, Skill.READING, "Coral Ecology");
        JobStatusResponse response = orchestrator.startJob(request, adminId);

        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertEquals(GenerationState.PENDING_REVIEW, response.practiceState());
        verify(repository).updateSetState(any(), org.mockito.ArgumentMatchers.eq(GenerationState.PENDING_REVIEW), any());
    }

    @Test
    void blocksJobCreationWhenRightsGuardFails() {
        UUID sourceId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        when(rightsGuard.verifySourceCanGenerate(sourceId))
                .thenThrow(new SourceRightsException("Rights not approved"));

        CreateJobRequest request = new CreateJobRequest(sourceId, null, Skill.READING, "Topic");
        assertThrows(SourceRightsException.class, () -> orchestrator.startJob(request, adminId));
    }
}
