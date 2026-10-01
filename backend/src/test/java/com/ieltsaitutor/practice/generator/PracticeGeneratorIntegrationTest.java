package com.ieltsaitutor.practice.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.routing.AiProviderRouter;
import com.ieltsaitutor.practice.generator.ai.PracticeSynthesisParser;
import com.ieltsaitutor.practice.generator.ai.ReadingPracticeGeneratorService;
import com.ieltsaitutor.practice.generator.ai.ReadingPromptBuilder;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintExtractorService;
import com.ieltsaitutor.practice.generator.blueprint.DefaultBlueprintCatalog;
import com.ieltsaitutor.practice.generator.critic.AiCriticService;
import com.ieltsaitutor.practice.generator.domain.GenerationState;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.dto.CreateJobRequest;
import com.ieltsaitutor.practice.generator.dto.JobStatusResponse;
import com.ieltsaitutor.practice.generator.exception.SourceRightsException;
import com.ieltsaitutor.practice.generator.repository.JdbcPracticeGenerationRepository;
import com.ieltsaitutor.practice.generator.service.GenerationJobOrchestrator;
import com.ieltsaitutor.practice.generator.service.GenerationStateMachine;
import com.ieltsaitutor.practice.generator.service.SourceNormalizationService;
import com.ieltsaitutor.practice.generator.service.SourceRightsGuard;
import com.ieltsaitutor.practice.generator.similarity.ContiguousSequenceDetector;
import com.ieltsaitutor.practice.generator.similarity.EntityRetentionFilter;
import com.ieltsaitutor.practice.generator.similarity.NGramOverlapCalculator;
import com.ieltsaitutor.practice.generator.similarity.SimilarityPolicyRegistry;
import com.ieltsaitutor.practice.generator.similarity.SimilarityValidator;
import com.ieltsaitutor.practice.generator.validator.AcademicWordListIndex;
import com.ieltsaitutor.practice.generator.validator.AmbiguityValidator;
import com.ieltsaitutor.practice.generator.validator.AnswerKeyValidator;
import com.ieltsaitutor.practice.generator.validator.DefaultValidationPipelineEngine;
import com.ieltsaitutor.practice.generator.validator.DifficultyValidator;
import com.ieltsaitutor.practice.generator.validator.EvidenceSpanValidator;
import com.ieltsaitutor.practice.generator.validator.PracticeValidator;
import com.ieltsaitutor.practice.generator.validator.ReadabilityMetricsCalculator;
import com.ieltsaitutor.practice.generator.validator.StructuralValidator;
import com.ieltsaitutor.practice.generator.validator.ValidationPipelineEngine;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.domain.Skill;

class PracticeGeneratorIntegrationTest {

    private SourceNormalizationService normalizationService;
    private BlueprintExtractorService blueprintExtractorService;
    private AiProviderRouter aiRouter;
    private ReadingPracticeGeneratorService generatorService;
    private GenerationStateMachine stateMachine;
    private ValidationPipelineEngine validationEngine;

    @BeforeEach
    void setUp() {
        normalizationService = new SourceNormalizationService();
        DefaultBlueprintCatalog catalog = new DefaultBlueprintCatalog();
        blueprintExtractorService = new BlueprintExtractorService(catalog, normalizationService);

        aiRouter = mock(AiProviderRouter.class);
        ReadingPromptBuilder promptBuilder = new ReadingPromptBuilder();
        PracticeSynthesisParser parser = new PracticeSynthesisParser();
        generatorService = new ReadingPracticeGeneratorService(aiRouter, promptBuilder, parser);

        stateMachine = new GenerationStateMachine();

        List<PracticeValidator> validators = List.of(
                new StructuralValidator(),
                new AnswerKeyValidator(),
                new EvidenceSpanValidator(),
                new AmbiguityValidator(),
                new SimilarityValidator(new SimilarityPolicyRegistry(), new NGramOverlapCalculator(), new ContiguousSequenceDetector(), new EntityRetentionFilter()),
                new DifficultyValidator(new ReadabilityMetricsCalculator(), new AcademicWordListIndex())
        );
        AiCriticService criticService = new AiCriticService(aiRouter);
        validationEngine = new DefaultValidationPipelineEngine(validators, criticService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void endToEndApprovedGenerationPipelineYieldsPendingReview() {
        UUID sourceId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Instant now = Instant.now();

        String rawSource = "Academic investigation into sub-polar estuarine sediments. ".repeat(60);
        String normalized = normalizationService.normalizeText(rawSource);
        String checksum = normalizationService.computeChecksum(normalized);

        PracticeGenerationSource source = new PracticeGenerationSource(
                sourceId, "Arctic Estuary Sediments", "PASTED_TEXT", "Researcher",
                RightsStatus.APPROVED, "CC-BY 4.0 Open Access", normalized, checksum, adminId, now, now);

        NamedParameterJdbcTemplate mockJdbc = mock(NamedParameterJdbcTemplate.class);
        when(mockJdbc.query(contains("practice_generation_sources WHERE id = :id"),
                any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(source));

        JdbcPracticeGenerationRepository testRepo = new JdbcPracticeGenerationRepository(mockJdbc);
        SourceRightsGuard testGuard = new SourceRightsGuard(testRepo);
        GenerationJobOrchestrator testOrchestrator = new GenerationJobOrchestrator(
                testRepo, testGuard, blueprintExtractorService, generatorService, stateMachine, validationEngine);

        String generatedJson = """
                {
                  "title": "Arctic Estuarine Microplastics",
                  "passage": {
                    "title": "Arctic Estuarine Microplastics",
                    "paragraphs": [
                      { "id": "p1", "text": "In high-latitude river deltas, sediment sampling has uncovered unexpected plastic particulate deposition. Recent surveys in sub-polar estuaries reveal significant accumulations across benthic layers." },
                      { "id": "p2", "text": "Contrary to initial hypotheses that sea ice prevents settling, seasonal melting accelerates particulate descent. Scientists recommend comprehensive ecological monitoring protocols." }
                    ]
                  },
                  "questions": [
                    {
                      "id": "q1",
                      "taskType": "TRUE_FALSE_NOT_GIVEN",
                      "prompt": "Seasonal melting increases the rate at which plastic particles descend.",
                      "options": ["TRUE", "FALSE", "NOT GIVEN"],
                      "answerKey": "TRUE",
                      "evidenceSpan": "seasonal melting accelerates particulate descent",
                      "explanation": "Paragraph 2 explicitly confirms that melting accelerates descent."
                    },
                    {
                      "id": "q2",
                      "taskType": "MULTIPLE_CHOICE",
                      "prompt": "What do scientists advise regarding sub-polar estuaries?",
                      "options": ["Complete commercial ban", "Comprehensive ecological monitoring protocols", "Immediate dredging", "Artificial ice formation"],
                      "answerKey": "B",
                      "evidenceSpan": "Scientists recommend comprehensive ecological monitoring protocols",
                      "explanation": "Paragraph 2 states the exact monitoring recommendation."
                    }
                  ]
                }
                """;

        when(aiRouter.chat(any(AiChatCommand.class)))
                .thenReturn(AiChatResult.answered(generatedJson));

        CreateJobRequest request = new CreateJobRequest(sourceId, null, Skill.READING, "Arctic Estuarine Microplastics");
        JobStatusResponse response = testOrchestrator.startJob(request, adminId);

        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertEquals(GenerationState.PENDING_REVIEW, response.practiceState());
    }

    @Test
    @SuppressWarnings("unchecked")
    void endToEndRejectsUnapprovedRightsStatus() {
        UUID sourceId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Instant now = Instant.now();

        PracticeGenerationSource source = new PracticeGenerationSource(
                sourceId, "Unapproved Copyrighted Source", "PASTED_TEXT", "Commercial Exam Press",
                RightsStatus.PENDING_REVIEW, "Copyrighted", "Content...", "hash", adminId, now, now);

        NamedParameterJdbcTemplate mockJdbc = mock(NamedParameterJdbcTemplate.class);
        when(mockJdbc.query(contains("practice_generation_sources WHERE id = :id"),
                any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(source));

        JdbcPracticeGenerationRepository testRepo = new JdbcPracticeGenerationRepository(mockJdbc);
        SourceRightsGuard testGuard = new SourceRightsGuard(testRepo);
        GenerationJobOrchestrator testOrchestrator = new GenerationJobOrchestrator(
                testRepo, testGuard, blueprintExtractorService, generatorService, stateMachine, validationEngine);

        CreateJobRequest request = new CreateJobRequest(sourceId, null, Skill.READING, "Topic");
        assertThrows(SourceRightsException.class, () -> testOrchestrator.startJob(request, adminId));
    }
}
