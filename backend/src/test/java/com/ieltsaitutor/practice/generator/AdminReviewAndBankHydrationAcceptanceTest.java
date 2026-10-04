package com.ieltsaitutor.practice.generator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.routing.AiProviderRouter;
import com.ieltsaitutor.practice.PracticeAttemptResult;
import com.ieltsaitutor.practice.PracticeAttemptStore;
import com.ieltsaitutor.practice.PracticeService;
import com.ieltsaitutor.practice.PracticeSet;
import com.ieltsaitutor.practice.SyntheticPracticeCatalog;
import com.ieltsaitutor.practice.generator.ai.PracticeSynthesisParser;
import com.ieltsaitutor.practice.generator.ai.ReadingPracticeGeneratorService;
import com.ieltsaitutor.practice.generator.ai.ReadingPromptBuilder;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintExtractorService;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.blueprint.DefaultBlueprintCatalog;
import com.ieltsaitutor.practice.generator.blueprint.ItemDistributionSpec;
import com.ieltsaitutor.practice.generator.blueprint.PassageStructureSpec;
import com.ieltsaitutor.practice.generator.blueprint.ReasoningRulesSpec;
import com.ieltsaitutor.practice.generator.critic.AiCriticService;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeSet;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeVersion;
import com.ieltsaitutor.practice.generator.domain.GenerationState;
import com.ieltsaitutor.practice.generator.domain.GenerationValidationResult;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationBlueprint;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationJob;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.PracticeReviewAction;
import com.ieltsaitutor.practice.generator.dto.CreateJobRequest;
import com.ieltsaitutor.practice.generator.dto.GeneratedSetReviewPayload;
import com.ieltsaitutor.practice.generator.dto.JobStatusResponse;
import com.ieltsaitutor.practice.generator.dto.ReviewActionRequest;
import com.ieltsaitutor.practice.generator.dto.ReviewActionResponse;
import com.ieltsaitutor.practice.generator.repository.PracticeGenerationRepository;
import com.ieltsaitutor.practice.generator.repository.PracticeProvenanceRepository;
import com.ieltsaitutor.practice.generator.service.DefaultPracticeBankHydrationService;
import com.ieltsaitutor.practice.generator.service.DefaultPracticeReviewService;
import com.ieltsaitutor.practice.generator.service.GenerationJobOrchestrator;
import com.ieltsaitutor.practice.generator.service.GenerationStateMachine;
import com.ieltsaitutor.practice.generator.service.PracticeAuditService;
import com.ieltsaitutor.practice.generator.service.PracticeProvenanceRecord;
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
import com.ieltsaitutor.practice.generator.validator.ReadabilityMetricsCalculator;
import com.ieltsaitutor.practice.generator.validator.StructuralValidator;
import com.ieltsaitutor.practice.generator.validator.ValidationPipelineEngine;
import com.ieltsaitutor.practice.repository.DatabasePracticeCatalogStore;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.domain.Skill;

class AdminReviewAndBankHydrationAcceptanceTest {

    private InMemoryPracticeGenerationRepository repo;
    private InMemoryProvenanceRepository provRepo;
    private DatabasePracticeCatalogStore catalogStore;
    private SyntheticPracticeCatalog studentCatalog;
    private PracticeService practiceService;
    private GenerationJobOrchestrator orchestrator;
    private DefaultPracticeReviewService reviewService;
    private DefaultPracticeBankHydrationService hydrationService;
    private AiProviderRouter router;

    @BeforeEach
    void setUp() {
        repo = new InMemoryPracticeGenerationRepository();
        provRepo = new InMemoryProvenanceRepository();
        catalogStore = new DatabasePracticeCatalogStore();
        studentCatalog = new SyntheticPracticeCatalog(catalogStore);
        PracticeAttemptStore attemptStore = mock(PracticeAttemptStore.class);
        when(attemptStore.saveAndReturn(any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(UUID.randomUUID());
        practiceService = new PracticeService(studentCatalog, attemptStore);

        SourceRightsGuard rightsGuard = new SourceRightsGuard(repo);
        DefaultBlueprintCatalog blueprintCatalog = new DefaultBlueprintCatalog();
        SourceNormalizationService normService = new SourceNormalizationService();
        BlueprintExtractorService blueprintExtractor = new BlueprintExtractorService(blueprintCatalog, normService);

        router = mock(AiProviderRouter.class);
        ReadingPromptBuilder promptBuilder = new ReadingPromptBuilder();
        PracticeSynthesisParser synthesisParser = new PracticeSynthesisParser();
        ReadingPracticeGeneratorService generatorService = new ReadingPracticeGeneratorService(router, promptBuilder, synthesisParser);

        GenerationStateMachine stateMachine = new GenerationStateMachine();

        // Build validators
        StructuralValidator structVal = new StructuralValidator();
        AnswerKeyValidator keyVal = new AnswerKeyValidator();
        EvidenceSpanValidator spanVal = new EvidenceSpanValidator();
        AmbiguityValidator ambVal = new AmbiguityValidator();
        AcademicWordListIndex awl = new AcademicWordListIndex();
        ReadabilityMetricsCalculator readCalc = new ReadabilityMetricsCalculator();
        DifficultyValidator diffVal = new DifficultyValidator(readCalc, awl);
        SimilarityPolicyRegistry simReg = new SimilarityPolicyRegistry();
        NGramOverlapCalculator ngram = new NGramOverlapCalculator();
        ContiguousSequenceDetector seq = new ContiguousSequenceDetector();
        EntityRetentionFilter ent = new EntityRetentionFilter();
        SimilarityValidator simVal = new SimilarityValidator(simReg, ngram, seq, ent);
        AiCriticService critic = new AiCriticService(router);

        ValidationPipelineEngine validationEngine = new DefaultValidationPipelineEngine(
                List.of(structVal, keyVal, spanVal, ambVal, diffVal, simVal), critic
        );

        orchestrator = new GenerationJobOrchestrator(
                repo, rightsGuard, blueprintExtractor, generatorService, stateMachine, validationEngine
        );

        hydrationService = new DefaultPracticeBankHydrationService(catalogStore, provRepo, repo);
        PracticeAuditService auditService = new PracticeAuditService(repo);
        reviewService = new DefaultPracticeReviewService(repo, auditService, hydrationService, stateMachine);
    }

    @Test
    void completeEndToEndLifecycleFromSourceToStudentAttempt() {
        UUID adminId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        // 1. Register Source
        SourceNormalizationService normService = new SourceNormalizationService();
        String sourceRaw = "Global warming is altering polar ecosystems drastically. " +
                "Scientists observed temperature shifts and loss of ice sheets. " +
                "These shifts threaten native fauna including penguins and seals. " +
                "Urgent international policy intervention is necessary to preserve biodiversity. " +
                "Researchers emphasize that climate changes occur faster than predicted.";
        StringBuilder padded = new StringBuilder(sourceRaw);
        for (int i = 0; i < 40; i++) {
            padded.append(" Furthermore, polar atmospheric research indicates long term patterns across Antarctic sectors.");
        }
        String cleanText = normService.normalizeText(padded.toString());
        PracticeGenerationSource source = new PracticeGenerationSource(
                UUID.randomUUID(), "Polar Climate Research", "TEXT", "ADMIN",
                RightsStatus.APPROVED, "Licensed open research", cleanText,
                normService.computeChecksum(cleanText), adminId, Instant.now(), Instant.now()
        );
        repo.saveSource(source);

        // 2. Blueprint extraction
        BlueprintSchema bpSchema = new BlueprintSchema(
                "bp-polar-1", Skill.READING, BigDecimal.valueOf(7.5), "ENVIRONMENTAL_SCIENCE",
                new PassageStructureSpec(500, 4, "EXPOSITORY", "ACADEMIC_C1"),
                List.of(
                        new ItemDistributionSpec("TRUE_FALSE_NOT_GIVEN", 2, List.of(1, 2), "FACTUAL_VERIFICATION"),
                        new ItemDistributionSpec("MULTIPLE_CHOICE", 1, List.of(3), "MAIN_IDEA")
                ),
                new ReasoningRulesSpec(true, true, 2)
        );
        PracticeGenerationBlueprint blueprint = new PracticeGenerationBlueprint(
                UUID.randomUUID(), Skill.READING, "Polar Blueprint", BigDecimal.valueOf(7.5),
                new BlueprintExtractorService(new DefaultBlueprintCatalog(), normService).toJson(bpSchema),
                adminId, Instant.now(), Instant.now()
        );
        repo.saveBlueprint(blueprint);

        // 3. Mock AI synthesis & critic response
        String synthesizedJson = """
        {
          "title": "Changing Realities of Polar Ecosystems",
          "passage": {
            "title": "Changing Realities of Polar Ecosystems",
            "paragraphs": [
              { "id": "p1", "text": "Global climate transformations are drastically modifying polar habitats and coastal zones." },
              { "id": "p2", "text": "Marine biologists recorded unprecedented declines in perennial sea ice covers across southern latitudes." },
              { "id": "p3", "text": "Endangered species such as Adelie penguins face severe reductions in their primary foraging grounds." },
              { "id": "p4", "text": "Coordinated multinational conservation strategies are required to stabilize these vulnerable marine ecosystems." }
            ]
          },
          "questions": [
            {
              "id": "q1",
              "taskType": "TRUE_FALSE_NOT_GIVEN",
              "prompt": "Polar habitats are currently experiencing drastic modifications.",
              "options": ["TRUE", "FALSE", "NOT GIVEN"],
              "answerKey": "TRUE",
              "evidenceSpan": "drastically modifying polar habitats",
              "explanation": "Paragraph 1 confirms drastic modifications."
            },
            {
              "id": "q2",
              "taskType": "TRUE_FALSE_NOT_GIVEN",
              "prompt": "Penguin populations have completely migrated away from all polar regions.",
              "options": ["TRUE", "FALSE", "NOT GIVEN"],
              "answerKey": "NOT GIVEN",
              "evidenceSpan": "Adelie penguins face severe reductions in their primary foraging grounds",
              "explanation": "No information about total migration is stated."
            },
            {
              "id": "q3",
              "taskType": "MULTIPLE_CHOICE",
              "prompt": "What is the primary conclusion regarding conservation strategies?",
              "options": ["They are unnecessary", "Multinational coordination is required", "Local laws suffice", "Funding is fully secured"],
              "answerKey": "B",
              "evidenceSpan": "Coordinated multinational conservation strategies are required",
              "explanation": "Paragraph 4 emphasizes multinational coordination."
            }
          ]
        }
        """;

        when(router.chat(any())).thenReturn(
                AiChatResult.answered(synthesizedJson),
                AiChatResult.answered("{\"overallCritique\":\"High quality academic passage.\",\"confidenceScore\":0.92,\"itemFeedback\":[]}")
        );

        // 4. Start Generation Job
        JobStatusResponse jobResponse = orchestrator.startJob(
                new CreateJobRequest(source.id(), blueprint.id(), Skill.READING, "Polar ecology"),
                adminId
        );

        assertNotNull(jobResponse);
        assertNotNull(jobResponse.jobId());
        assertNotNull(jobResponse.setId());
        assertEquals(GenerationState.PENDING_REVIEW, jobResponse.practiceState());

        // 5. Verify unapproved set is NOT in student catalog
        assertEquals(1, practiceService.sets("reading").size());
        assertThrows(IllegalArgumentException.class, () -> practiceService.set("reading", jobResponse.setId().toString()));

        // 6. Review payload inspection
        GeneratedSetReviewPayload reviewPayload = reviewService.getReviewPayload(jobResponse.setId());
        assertNotNull(reviewPayload);
        assertEquals(GenerationState.PENDING_REVIEW, reviewPayload.practiceSet().state());
        assertEquals(1, reviewPayload.currentVersion().versionNumber());

        // 7. Human Review Action: APPROVE
        ReviewActionResponse approvalResponse = reviewService.executeReview(
                jobResponse.setId(),
                new ReviewActionRequest("APPROVE", "Passed editorial and academic criteria", null, null),
                adminId
        );

        assertNotNull(approvalResponse);
        assertEquals("APPROVE", approvalResponse.action());
        assertEquals(GenerationState.APPROVED, approvalResponse.resultingState());

        // 8. Verify Bank Hydration
        List<PracticeSet> studentReadingSets = practiceService.sets("reading");
        assertEquals(2, studentReadingSets.size());

        GeneratedPracticeSet updatedSet = repo.findSetById(jobResponse.setId()).orElseThrow();
        String publishedSetId = updatedSet.publishedSetId();
        assertNotNull(publishedSetId);

        PracticeSet publishedPracticeSet = practiceService.set("reading", publishedSetId);
        assertNotNull(publishedPracticeSet);
        assertEquals(3, publishedPracticeSet.questions().size());
        assertNotNull(publishedPracticeSet.passage());
        assertEquals(4, publishedPracticeSet.passage().paragraphs().size());

        // 9. Student Attempt Submission & Scoring
        Map<String, String> answers = Map.of(
                publishedSetId + "-q1", "TRUE",
                publishedSetId + "-q2", "NOT GIVEN",
                publishedSetId + "-q3", "B"
        );
        PracticeAttemptResult attemptResult = practiceService.submit("reading", publishedSetId, answers, studentId);

        assertNotNull(attemptResult);
        assertEquals(3, attemptResult.score());
        assertEquals(3, attemptResult.total());
        assertEquals(3, attemptResult.review().size());
        assertTrue(attemptResult.review().stream().allMatch(com.ieltsaitutor.practice.PracticeReview::correct));

        // 10. Verify Provenance Record
        Optional<PracticeProvenanceRecord> prov = provRepo.findBySetId(publishedSetId);
        assertTrue(prov.isPresent());
        assertEquals(adminId, prov.get().approverId());
        assertEquals(jobResponse.jobId(), prov.get().generationJobId());
    }

    private static class InMemoryPracticeGenerationRepository implements PracticeGenerationRepository {
        private final Map<UUID, PracticeGenerationSource> sources = new ConcurrentHashMap<>();
        private final Map<UUID, PracticeGenerationBlueprint> blueprints = new ConcurrentHashMap<>();
        private final Map<UUID, PracticeGenerationJob> jobs = new ConcurrentHashMap<>();
        private final Map<UUID, GeneratedPracticeSet> sets = new ConcurrentHashMap<>();
        private final Map<UUID, GeneratedPracticeVersion> versions = new ConcurrentHashMap<>();
        private final Map<UUID, GenerationValidationResult> validationResults = new ConcurrentHashMap<>();
        private final Map<UUID, PracticeReviewAction> reviewActions = new ConcurrentHashMap<>();

        @Override public PracticeGenerationSource saveSource(PracticeGenerationSource source) { sources.put(source.id(), source); return source; }
        @Override public Optional<PracticeGenerationSource> findSourceById(UUID id) { return Optional.ofNullable(sources.get(id)); }
        @Override public Optional<PracticeGenerationSource> findSourceByChecksum(String checksum) { return sources.values().stream().filter(s -> s.checksum().equals(checksum)).findFirst(); }
        @Override public List<PracticeGenerationSource> listSources(RightsStatus filter) { return List.copyOf(sources.values()); }
        @Override public void updateSourceRights(UUID sourceId, RightsStatus status, String licenseNote) {}

        @Override public PracticeGenerationBlueprint saveBlueprint(PracticeGenerationBlueprint blueprint) { blueprints.put(blueprint.id(), blueprint); return blueprint; }
        @Override public Optional<PracticeGenerationBlueprint> findBlueprintById(UUID id) { return Optional.ofNullable(blueprints.get(id)); }
        @Override public List<PracticeGenerationBlueprint> listBlueprints(String skill) { return List.copyOf(blueprints.values()); }

        @Override public PracticeGenerationJob saveJob(PracticeGenerationJob job) { jobs.put(job.id(), job); return job; }
        @Override public Optional<PracticeGenerationJob> findJobById(UUID id) { return Optional.ofNullable(jobs.get(id)); }
        @Override public List<PracticeGenerationJob> listJobs(int limit, int offset) { return List.copyOf(jobs.values()); }
        @Override public void updateJobStatus(UUID jobId, String status, String errorMessage) {}

        @Override public GeneratedPracticeSet saveSet(GeneratedPracticeSet set) { sets.put(set.id(), set); return set; }
        @Override public Optional<GeneratedPracticeSet> findSetById(UUID id) { return Optional.ofNullable(sets.get(id)); }
        @Override public List<GeneratedPracticeSet> listSets(GenerationState filterState, int limit, int offset) { return List.copyOf(sets.values()); }
        @Override public void updateSetState(UUID setId, GenerationState state, UUID currentVersionId) {
            GeneratedPracticeSet set = sets.get(setId);
            if (set != null) {
                sets.put(setId, new GeneratedPracticeSet(setId, set.jobId(), set.skill(), set.title(), currentVersionId, state, set.publishedSetId(), set.approvedBy(), set.approvedAt(), set.createdAt(), Instant.now()));
            }
        }
        @Override public void markSetApproved(UUID setId, String publishedSetId, UUID approvedBy) {
            GeneratedPracticeSet set = sets.get(setId);
            if (set != null) {
                sets.put(setId, new GeneratedPracticeSet(setId, set.jobId(), set.skill(), set.title(), set.currentVersionId(), GenerationState.APPROVED, publishedSetId, approvedBy, Instant.now(), set.createdAt(), Instant.now()));
            }
        }

        @Override public GeneratedPracticeVersion saveVersion(GeneratedPracticeVersion version) { versions.put(version.id(), version); return version; }
        @Override public Optional<GeneratedPracticeVersion> findVersionById(UUID id) { return Optional.ofNullable(versions.get(id)); }
        @Override public Optional<GeneratedPracticeVersion> findVersionBySetAndNumber(UUID setId, int versionNumber) {
            return versions.values().stream().filter(v -> v.setId().equals(setId) && v.versionNumber() == versionNumber).findFirst();
        }
        @Override public List<GeneratedPracticeVersion> listVersionsForSet(UUID setId) {
            return versions.values().stream().filter(v -> v.setId().equals(setId)).toList();
        }

        @Override public GenerationValidationResult saveValidationResult(GenerationValidationResult result) { validationResults.put(result.id(), result); return result; }
        @Override public List<GenerationValidationResult> listValidationResultsForVersion(UUID versionId) {
            return validationResults.values().stream().filter(v -> v.versionId().equals(versionId)).toList();
        }

        @Override public PracticeReviewAction saveReviewAction(PracticeReviewAction action) { reviewActions.put(action.id(), action); return action; }
        @Override public List<PracticeReviewAction> listReviewActionsForSet(UUID setId) {
            return reviewActions.values().stream().filter(a -> a.setId().equals(setId)).toList();
        }
    }

    private static class InMemoryProvenanceRepository implements PracticeProvenanceRepository {
        private final Map<String, PracticeProvenanceRecord> records = new ConcurrentHashMap<>();
        @Override public PracticeProvenanceRecord save(PracticeProvenanceRecord record) { records.put(record.setId(), record); return record; }
        @Override public Optional<PracticeProvenanceRecord> findBySetId(String setId) { return Optional.ofNullable(records.get(setId)); }
    }
}
