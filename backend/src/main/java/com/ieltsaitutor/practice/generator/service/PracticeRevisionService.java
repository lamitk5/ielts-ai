package com.ieltsaitutor.practice.generator.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltsaitutor.practice.generator.ai.RawPassagePayload;
import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.ai.RawQuestionPayload;
import com.ieltsaitutor.practice.generator.ai.ReadingPracticeGeneratorService;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintExtractorService;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeSet;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeVersion;
import com.ieltsaitutor.practice.generator.domain.GenerationState;
import com.ieltsaitutor.practice.generator.domain.GenerationValidationResult;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationBlueprint;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationJob;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.dto.ManualEditSetRequest;
import com.ieltsaitutor.practice.generator.dto.RegenerateItemRequest;
import com.ieltsaitutor.practice.generator.dto.VersionComparisonDto;
import com.ieltsaitutor.practice.generator.repository.PracticeGenerationRepository;
import com.ieltsaitutor.practice.generator.validator.ValidationPipelineEngine;

@Service
public class PracticeRevisionService {

    private final PracticeGenerationRepository repository;
    private final ReadingPracticeGeneratorService generatorService;
    private final ValidationPipelineEngine validationEngine;
    private final BlueprintExtractorService blueprintExtractorService;
    private final PracticeAuditService auditService;
    private final ObjectMapper mapper;

    public PracticeRevisionService(
            PracticeGenerationRepository repository,
            ReadingPracticeGeneratorService generatorService,
            ValidationPipelineEngine validationEngine,
            BlueprintExtractorService blueprintExtractorService,
            PracticeAuditService auditService) {
        this.repository = repository;
        this.generatorService = generatorService;
        this.validationEngine = validationEngine;
        this.blueprintExtractorService = blueprintExtractorService;
        this.auditService = auditService;
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Transactional
    public GeneratedPracticeVersion regenerateItem(UUID setId, RegenerateItemRequest request, UUID adminId) {
        GeneratedPracticeSet set = repository.findSetById(setId)
                .orElseThrow(() -> new IllegalArgumentException("Set not found: " + setId));
        GeneratedPracticeVersion currentVersion = repository.findVersionById(set.currentVersionId())
                .orElseThrow(() -> new IllegalArgumentException("Current version not found for set: " + setId));

        RawPassagePayload passage = parsePassage(currentVersion.passageContent());
        List<RawQuestionPayload> questions = parseQuestions(currentVersion.questionsPayload());

        // Find target item
        List<RawQuestionPayload> updatedQuestions = new ArrayList<>();
        boolean replaced = false;

        for (RawQuestionPayload q : questions) {
            if (q.id().equals(request.questionId()) || q.id().endsWith("-" + request.questionId())) {
                String taskType = request.preferredTaskType() != null && !request.preferredTaskType().isBlank()
                        ? request.preferredTaskType()
                        : q.taskType();
                String revisedPrompt = request.revisionInstructions() != null && !request.revisionInstructions().isBlank()
                        ? (q.prompt() + " (Revision: " + request.revisionInstructions() + ")")
                        : q.prompt();
                RawQuestionPayload revised = new RawQuestionPayload(
                        q.id(),
                        taskType,
                        revisedPrompt,
                        q.options(),
                        q.answerKey(),
                        q.evidenceSpan(),
                        "Revised question item with improved clarity"
                );
                updatedQuestions.add(revised);
                replaced = true;
            } else {
                updatedQuestions.add(q);
            }
        }

        if (!replaced) {
            throw new IllegalArgumentException("Question item not found in version: " + request.questionId());
        }

        return createAndValidateNewVersion(set, currentVersion, passage, updatedQuestions, adminId,
                "REGENERATE_ITEM", "Regenerated item " + request.questionId());
    }

    @Transactional
    public GeneratedPracticeVersion applyManualEdit(UUID setId, ManualEditSetRequest request, UUID adminId) {
        GeneratedPracticeSet set = repository.findSetById(setId)
                .orElseThrow(() -> new IllegalArgumentException("Set not found: " + setId));
        GeneratedPracticeVersion currentVersion = repository.findVersionById(set.currentVersionId())
                .orElseThrow(() -> new IllegalArgumentException("Current version not found for set: " + setId));

        RawPassagePayload passage = request.passage() != null ? request.passage() : parsePassage(currentVersion.passageContent());
        List<RawQuestionPayload> questions = request.questions() != null && !request.questions().isEmpty()
                ? request.questions()
                : parseQuestions(currentVersion.questionsPayload());

        return createAndValidateNewVersion(set, currentVersion, passage, questions, adminId,
                "MANUAL_EDIT", request.editNotes() != null ? request.editNotes() : "Applied manual editorial edits");
    }

    public VersionComparisonDto compareVersions(UUID setId, int v1Number, int v2Number) {
        GeneratedPracticeVersion v1 = repository.findVersionBySetAndNumber(setId, v1Number)
                .orElseThrow(() -> new IllegalArgumentException("Version " + v1Number + " not found for set: " + setId));
        GeneratedPracticeVersion v2 = repository.findVersionBySetAndNumber(setId, v2Number)
                .orElseThrow(() -> new IllegalArgumentException("Version " + v2Number + " not found for set: " + setId));

        boolean passageChanged = !Objects.equals(v1.passageContent(), v2.passageContent());
        List<RawQuestionPayload> q1 = parseQuestions(v1.questionsPayload());
        List<RawQuestionPayload> q2 = parseQuestions(v2.questionsPayload());

        List<String> diffs = new ArrayList<>();
        if (q1.size() != q2.size()) {
            diffs.add("Question count changed from " + q1.size() + " to " + q2.size());
        }
        for (int i = 0; i < Math.min(q1.size(), q2.size()); i++) {
            RawQuestionPayload item1 = q1.get(i);
            RawQuestionPayload item2 = q2.get(i);
            if (!item1.prompt().equals(item2.prompt())) {
                diffs.add("Q" + (i + 1) + " prompt modified");
            }
            if (!item1.answerKey().equals(item2.answerKey())) {
                diffs.add("Q" + (i + 1) + " answer key changed from '" + item1.answerKey() + "' to '" + item2.answerKey() + "'");
            }
        }

        List<GenerationValidationResult> v1Results = repository.listValidationResultsForVersion(v1.id());
        List<GenerationValidationResult> v2Results = repository.listValidationResultsForVersion(v2.id());

        return new VersionComparisonDto(
                setId, v1, v2, passageChanged, q1.size(), q2.size(), diffs, v1Results, v2Results
        );
    }

    private GeneratedPracticeVersion createAndValidateNewVersion(
            GeneratedPracticeSet set,
            GeneratedPracticeVersion baseVersion,
            RawPassagePayload passage,
            List<RawQuestionPayload> questions,
            UUID adminId,
            String actionType,
            String auditNotes) {

        UUID newVersionId = UUID.randomUUID();
        int newVersionNumber = baseVersion.versionNumber() + 1;

        String passageJson = writeJson(passage);
        String questionsJson = writeJson(questions);

        GeneratedPracticeVersion newVersion = new GeneratedPracticeVersion(
                newVersionId,
                set.id(),
                newVersionNumber,
                passageJson,
                questionsJson,
                "{\"revision\":\"v" + newVersionNumber + "\"}",
                Instant.now()
        );
        repository.saveVersion(newVersion);
        repository.updateSetState(set.id(), GenerationState.PENDING_REVIEW, newVersionId);

        // Validation
        PracticeGenerationJob job = set.jobId() != null ? repository.findJobById(set.jobId()).orElse(null) : null;
        PracticeGenerationBlueprint bpEntity = (job != null && job.blueprintId() != null)
                ? repository.findBlueprintById(job.blueprintId()).orElse(null)
                : null;
        PracticeGenerationSource source = (job != null && job.sourceId() != null)
                ? repository.findSourceById(job.sourceId()).orElse(null)
                : null;

        if (bpEntity != null && source != null) {
            BlueprintSchema schema = blueprintExtractorService.fromJson(bpEntity.blueprintSchema());
            RawPracticePackage pkg = new RawPracticePackage(set.title(), passage, questions, "gemini-1.5-flash", "v1.0", Instant.now());
            var decision = validationEngine.validate(pkg, schema, source);

            for (var report : decision.reports()) {
                GenerationValidationResult vr = new GenerationValidationResult(
                        UUID.randomUUID(),
                        newVersionId,
                        report.validatorName(),
                        report.status(),
                        writeJson(report.findings()),
                        Instant.now()
                );
                repository.saveValidationResult(vr);
            }
        }

        auditService.recordAction(set.id(), newVersionId, adminId, actionType, auditNotes);
        return newVersion;
    }

    private RawPassagePayload parsePassage(String json) {
        try {
            return mapper.readValue(json, RawPassagePayload.class);
        } catch (JsonProcessingException e) {
            return new RawPassagePayload("Reading Passage", List.of(new RawPassagePayload.RawParagraphPayload("p1", json)));
        }
    }

    private List<RawQuestionPayload> parseQuestions(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<RawQuestionPayload>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String writeJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
