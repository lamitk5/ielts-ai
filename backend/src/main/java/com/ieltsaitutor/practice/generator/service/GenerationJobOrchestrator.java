package com.ieltsaitutor.practice.generator.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.ai.ReadingPracticeGeneratorService;
import com.ieltsaitutor.practice.generator.ai.ReadingPromptBuilder;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintExtractorService;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeSet;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeVersion;
import com.ieltsaitutor.practice.generator.domain.GenerationState;
import com.ieltsaitutor.practice.generator.domain.GenerationValidationResult;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationBlueprint;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationJob;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;
import com.ieltsaitutor.practice.generator.dto.CreateJobRequest;
import com.ieltsaitutor.practice.generator.dto.JobStatusResponse;
import com.ieltsaitutor.practice.generator.exception.SourceRightsException;
import com.ieltsaitutor.practice.generator.repository.PracticeGenerationRepository;
import com.ieltsaitutor.practice.generator.validator.ValidationPipelineEngine;
import com.ieltsaitutor.rag.domain.Skill;

@Service
public class GenerationJobOrchestrator {

    private final PracticeGenerationRepository repository;
    private final SourceRightsGuard rightsGuard;
    private final BlueprintExtractorService blueprintExtractorService;
    private final ReadingPracticeGeneratorService generatorService;
    private final GenerationStateMachine stateMachine;
    private final ValidationPipelineEngine validationEngine;
    private final ObjectMapper mapper;

    public GenerationJobOrchestrator(
            PracticeGenerationRepository repository,
            SourceRightsGuard rightsGuard,
            BlueprintExtractorService blueprintExtractorService,
            ReadingPracticeGeneratorService generatorService,
            GenerationStateMachine stateMachine,
            ValidationPipelineEngine validationEngine) {
        this.repository = repository;
        this.rightsGuard = rightsGuard;
        this.blueprintExtractorService = blueprintExtractorService;
        this.generatorService = generatorService;
        this.stateMachine = stateMachine;
        this.validationEngine = validationEngine;
        this.mapper = new ObjectMapper();
    }

    public JobStatusResponse startJob(CreateJobRequest request, UUID adminId) {
        // 1. Rights pre-check
        PracticeGenerationSource source = rightsGuard.verifySourceCanGenerate(request.sourceId());

        // 2. Blueprint load/extract
        BlueprintSchema schema;
        UUID effectiveBlueprintId;
        if (request.blueprintId() != null) {
            UUID requestedBpId = request.blueprintId();
            PracticeGenerationBlueprint bpEntity = repository.findBlueprintById(requestedBpId)
                    .orElseThrow(() -> new IllegalArgumentException("Blueprint not found: " + requestedBpId));
            schema = blueprintExtractorService.fromJson(bpEntity.blueprintSchema());
            effectiveBlueprintId = requestedBpId;
        } else {
            schema = blueprintExtractorService.extractFromSource(source, request.skill(), null);
            PracticeGenerationBlueprint bpEntity = new PracticeGenerationBlueprint(
                    UUID.randomUUID(), request.skill(), "Auto-derived Blueprint: " + source.title(),
                    schema.targetBand(), blueprintExtractorService.toJson(schema), adminId, Instant.now(), Instant.now());
            repository.saveBlueprint(bpEntity);
            effectiveBlueprintId = bpEntity.id();
        }

        // 3. Create Job & Set entities
        UUID jobId = UUID.randomUUID();
        UUID setId = UUID.randomUUID();
        Instant now = Instant.now();

        PracticeGenerationJob job = new PracticeGenerationJob(
                jobId, source.id(), effectiveBlueprintId, request.skill(), "GENERATING", "gemini-1.5-flash",
                ReadingPromptBuilder.TEMPLATE_VERSION, null, adminId, now, null, now);
        repository.saveJob(job);

        GeneratedPracticeSet set = new GeneratedPracticeSet(
                setId, jobId, request.skill(), source.title() + " Practice", null,
                GenerationState.GENERATING, null, null, null, now, now);
        repository.saveSet(set);

        // 4. Execute generation & validation
        try {
            RawPracticePackage pkg = generatorService.generate(schema, request.domainTopic());

            // Create Version 1
            UUID versionId = UUID.randomUUID();
            String passageJson = mapper.writeValueAsString(pkg.passage());
            String questionsJson = mapper.writeValueAsString(pkg.questions());

            GeneratedPracticeVersion version = new GeneratedPracticeVersion(
                    versionId, setId, 1, passageJson, questionsJson, "{}", now);
            repository.saveVersion(version);

            // Advance state to AUTO_VALIDATING
            stateMachine.validateTransition(GenerationState.GENERATING, GenerationState.AUTO_VALIDATING);
            repository.updateSetState(setId, GenerationState.AUTO_VALIDATING, versionId);

            // Run Validation Pipeline
            ValidationPipelineEngine.AggregateValidationDecision decision = validationEngine.validate(pkg, schema, source);
            for (var report : decision.reports()) {
                String findingsJson = mapper.writeValueAsString(report.findings());
                GenerationValidationResult valResult = new GenerationValidationResult(
                        UUID.randomUUID(), versionId, report.validatorName(), report.status(), findingsJson, Instant.now());
                repository.saveValidationResult(valResult);
            }

            GenerationState nextState = (decision.status() == ValidationStatus.FAIL)
                    ? GenerationState.NEEDS_REVISION
                    : GenerationState.PENDING_REVIEW;

            stateMachine.validateTransition(GenerationState.AUTO_VALIDATING, nextState);
            repository.updateSetState(setId, nextState, versionId);
            repository.updateJobStatus(jobId, "COMPLETED", null);

            return new JobStatusResponse(jobId, setId, request.skill(), "COMPLETED", nextState, null, now, Instant.now());
        } catch (Exception e) {
            repository.updateJobStatus(jobId, "FAILED", e.getMessage());
            repository.updateSetState(setId, GenerationState.DRAFT, null);
            throw new RuntimeException("Generation job failed: " + e.getMessage(), e);
        }
    }
}
