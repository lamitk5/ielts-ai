package com.ieltsaitutor.practice.generator.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltsaitutor.practice.generator.ai.RawPassagePayload;
import com.ieltsaitutor.practice.generator.ai.RawQuestionPayload;
import com.ieltsaitutor.practice.generator.ai.ReadingPracticeGeneratorService;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintExtractorService;
import com.ieltsaitutor.practice.generator.blueprint.DefaultBlueprintCatalog;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeSet;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeVersion;
import com.ieltsaitutor.practice.generator.domain.GenerationState;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationBlueprint;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationJob;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;
import com.ieltsaitutor.practice.generator.dto.ManualEditSetRequest;
import com.ieltsaitutor.practice.generator.dto.RegenerateItemRequest;
import com.ieltsaitutor.practice.generator.dto.VersionComparisonDto;
import com.ieltsaitutor.practice.generator.repository.PracticeGenerationRepository;
import com.ieltsaitutor.practice.generator.validator.ValidationPipelineEngine;
import com.ieltsaitutor.practice.generator.validator.ValidationPipelineEngine.AggregateValidationDecision;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.domain.Skill;

class PracticeRevisionServiceTest {

    private PracticeGenerationRepository repository;
    private ReadingPracticeGeneratorService generatorService;
    private ValidationPipelineEngine validationEngine;
    private BlueprintExtractorService blueprintExtractorService;
    private PracticeAuditService auditService;
    private PracticeRevisionService revisionService;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        repository = mock(PracticeGenerationRepository.class);
        generatorService = mock(ReadingPracticeGeneratorService.class);
        validationEngine = mock(ValidationPipelineEngine.class);
        blueprintExtractorService = mock(BlueprintExtractorService.class);
        auditService = mock(PracticeAuditService.class);
        mapper = new ObjectMapper();

        revisionService = new PracticeRevisionService(
                repository, generatorService, validationEngine, blueprintExtractorService, auditService
        );
    }

    @Test
    void regenerateItemCreatesIncrementedVersionAndAudits() throws Exception {
        UUID setId = UUID.randomUUID();
        UUID v1Id = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID bpId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        RawPassagePayload passage = new RawPassagePayload("Test Passage", List.of(new RawPassagePayload.RawParagraphPayload("p1", "Paragraph 1 text.")));
        List<RawQuestionPayload> questions = List.of(
                new RawQuestionPayload("q1", "TRUE_FALSE_NOT_GIVEN", "Is this true?", List.of("TRUE", "FALSE", "NOT GIVEN"), "TRUE", "Paragraph 1 text.", "Explanation")
        );

        GeneratedPracticeSet set = new GeneratedPracticeSet(
                setId, jobId, Skill.READING, "Reading Set", v1Id, GenerationState.PENDING_REVIEW, null, null, null, Instant.now(), Instant.now()
        );
        GeneratedPracticeVersion v1 = new GeneratedPracticeVersion(
                v1Id, setId, 1, mapper.writeValueAsString(passage), mapper.writeValueAsString(questions), "{}", Instant.now()
        );

        PracticeGenerationJob job = new PracticeGenerationJob(
                jobId, sourceId, bpId, Skill.READING, "COMPLETED", "gemini", "v1", null, adminId, Instant.now(), null, Instant.now()
        );
        PracticeGenerationBlueprint bp = new PracticeGenerationBlueprint(
                bpId, Skill.READING, "BP", BigDecimal.valueOf(7.0), "{}", adminId, Instant.now(), Instant.now()
        );
        PracticeGenerationSource source = new PracticeGenerationSource(
                sourceId, "Source", "TEXT", "ADMIN", RightsStatus.APPROVED, "", "Content", "checksum", adminId, Instant.now(), Instant.now()
        );

        when(repository.findSetById(setId)).thenReturn(Optional.of(set));
        when(repository.findVersionById(v1Id)).thenReturn(Optional.of(v1));
        when(repository.findJobById(jobId)).thenReturn(Optional.of(job));
        when(repository.findBlueprintById(bpId)).thenReturn(Optional.of(bp));
        when(repository.findSourceById(sourceId)).thenReturn(Optional.of(source));
        when(blueprintExtractorService.fromJson(any())).thenReturn(new DefaultBlueprintCatalog().readingPassage1());
        when(validationEngine.validate(any(), any(), any()))
                .thenReturn(new AggregateValidationDecision(ValidationStatus.PASS, List.of(), "Valid"));

        GeneratedPracticeVersion v2 = revisionService.regenerateItem(
                setId, new RegenerateItemRequest("q1", "Clarify statement", null), adminId
        );

        assertNotNull(v2);
        assertEquals(2, v2.versionNumber());
        assertEquals(setId, v2.setId());
        verify(repository).saveVersion(any(GeneratedPracticeVersion.class));
        verify(repository).updateSetState(eq(setId), eq(GenerationState.PENDING_REVIEW), any(UUID.class));
        verify(auditService).recordAction(eq(setId), any(UUID.class), eq(adminId), eq("REGENERATE_ITEM"), any());
    }

    @Test
    void applyManualEditUpdatesVersionAndValidates() throws Exception {
        UUID setId = UUID.randomUUID();
        UUID v1Id = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        RawPassagePayload passage = new RawPassagePayload("Test Passage", List.of(new RawPassagePayload.RawParagraphPayload("p1", "Old text.")));
        List<RawQuestionPayload> questions = List.of(
                new RawQuestionPayload("q1", "TRUE_FALSE_NOT_GIVEN", "Prompt", List.of(), "TRUE", "Old text.", "")
        );

        GeneratedPracticeSet set = new GeneratedPracticeSet(
                setId, null, Skill.READING, "Reading Set", v1Id, GenerationState.PENDING_REVIEW, null, null, null, Instant.now(), Instant.now()
        );
        GeneratedPracticeVersion v1 = new GeneratedPracticeVersion(
                v1Id, setId, 1, mapper.writeValueAsString(passage), mapper.writeValueAsString(questions), "{}", Instant.now()
        );

        when(repository.findSetById(setId)).thenReturn(Optional.of(set));
        when(repository.findVersionById(v1Id)).thenReturn(Optional.of(v1));

        RawPassagePayload newPassage = new RawPassagePayload("Edited Passage", List.of(new RawPassagePayload.RawParagraphPayload("p1", "New text.")));
        GeneratedPracticeVersion v2 = revisionService.applyManualEdit(
                setId, new ManualEditSetRequest(newPassage, questions, "Edited passage title and text"), adminId
        );

        assertNotNull(v2);
        assertEquals(2, v2.versionNumber());
        assertTrue(v2.passageContent().contains("Edited Passage"));
        verify(repository).saveVersion(any(GeneratedPracticeVersion.class));
    }

    @Test
    void compareVersionsDetectsDiffs() throws Exception {
        UUID setId = UUID.randomUUID();
        UUID v1Id = UUID.randomUUID();
        UUID v2Id = UUID.randomUUID();

        RawPassagePayload p1 = new RawPassagePayload("Passage v1", List.of());
        RawPassagePayload p2 = new RawPassagePayload("Passage v2", List.of());

        List<RawQuestionPayload> q1 = List.of(
                new RawQuestionPayload("q1", "MULTIPLE_CHOICE", "Question 1 v1?", List.of("A", "B"), "A", "", "")
        );
        List<RawQuestionPayload> q2 = List.of(
                new RawQuestionPayload("q1", "MULTIPLE_CHOICE", "Question 1 v2 edited?", List.of("A", "B"), "B", "", "")
        );

        GeneratedPracticeVersion v1 = new GeneratedPracticeVersion(v1Id, setId, 1, mapper.writeValueAsString(p1), mapper.writeValueAsString(q1), "{}", Instant.now());
        GeneratedPracticeVersion v2 = new GeneratedPracticeVersion(v2Id, setId, 2, mapper.writeValueAsString(p2), mapper.writeValueAsString(q2), "{}", Instant.now());

        when(repository.findVersionBySetAndNumber(setId, 1)).thenReturn(Optional.of(v1));
        when(repository.findVersionBySetAndNumber(setId, 2)).thenReturn(Optional.of(v2));
        when(repository.listValidationResultsForVersion(v1Id)).thenReturn(List.of());
        when(repository.listValidationResultsForVersion(v2Id)).thenReturn(List.of());

        VersionComparisonDto comparison = revisionService.compareVersions(setId, 1, 2);

        assertNotNull(comparison);
        assertTrue(comparison.passageChanged());
        assertEquals(1, comparison.baseQuestionCount());
        assertEquals(1, comparison.compareQuestionCount());
        assertTrue(comparison.questionDifferences().stream().anyMatch(d -> d.contains("prompt modified")));
        assertTrue(comparison.questionDifferences().stream().anyMatch(d -> d.contains("answer key changed")));
    }
}
