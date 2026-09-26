package com.ieltsaitutor.practice.generator.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ieltsaitutor.practice.generator.blueprint.BlueprintExtractorService;
import com.ieltsaitutor.practice.generator.blueprint.DefaultBlueprintCatalog;
import com.ieltsaitutor.practice.generator.domain.GenerationState;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationBlueprint;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationJob;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.dto.JobStatusResponse;
import com.ieltsaitutor.practice.generator.repository.PracticeGenerationRepository;
import com.ieltsaitutor.practice.generator.service.GenerationJobOrchestrator;
import com.ieltsaitutor.practice.generator.service.SourceNormalizationService;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.domain.Skill;

class AdminPracticeGeneratorControllerTest {

    private GenerationJobOrchestrator jobOrchestrator;
    private PracticeGenerationRepository repository;
    private SourceNormalizationService normalizationService;
    private BlueprintExtractorService blueprintExtractorService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jobOrchestrator = mock(GenerationJobOrchestrator.class);
        repository = mock(PracticeGenerationRepository.class);
        normalizationService = mock(SourceNormalizationService.class);
        blueprintExtractorService = mock(BlueprintExtractorService.class);

        AdminPracticeGeneratorController controller = new AdminPracticeGeneratorController(
                jobOrchestrator, repository, normalizationService, blueprintExtractorService
        );
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void registerSourceNormalizesAndSaves() throws Exception {
        UUID sourceId = UUID.randomUUID();
        when(normalizationService.normalizeText(any())).thenReturn("Clean text of academic reading");
        when(normalizationService.computeChecksum(any())).thenReturn("hash123");
        when(normalizationService.countWords(any())).thenReturn(500);

        PracticeGenerationSource s = new PracticeGenerationSource(
                sourceId, "Academic Reading Text", "TEXT", "ADMIN",
                RightsStatus.APPROVED, "license", "Clean text of academic reading", "hash123",
                UUID.randomUUID(), Instant.now(), Instant.now()
        );
        when(repository.saveSource(any())).thenReturn(s);

        mvc.perform(post("/api/admin/practice-generator/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "title": "Academic Reading Text",
                            "rawText": "Clean text of academic reading",
                            "rightsStatus": "APPROVED",
                            "language": "en",
                            "skill": "reading"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Academic Reading Text"))
                .andExpect(jsonPath("$.rightsStatus").value("APPROVED"));
    }

    @Test
    void listSourcesReturnsAll() throws Exception {
        PracticeGenerationSource s = new PracticeGenerationSource(
                UUID.randomUUID(), "Source 1", "TEXT", "ADMIN", RightsStatus.APPROVED, "", "Clean text", "h1",
                UUID.randomUUID(), Instant.now(), Instant.now()
        );
        when(repository.listSources(any())).thenReturn(List.of(s));

        mvc.perform(get("/api/admin/practice-generator/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Source 1"));
    }

    @Test
    void extractBlueprintReturnsBlueprint() throws Exception {
        UUID bpId = UUID.randomUUID();
        PracticeGenerationBlueprint bp = new PracticeGenerationBlueprint(
                bpId, Skill.READING, "Climate Change Blueprint", BigDecimal.valueOf(7.5),
                "{}", UUID.randomUUID(), Instant.now(), Instant.now()
        );
        when(normalizationService.normalizeText(any())).thenReturn("This is raw text for extraction");
        when(normalizationService.computeChecksum(any())).thenReturn("checksum123");
        when(blueprintExtractorService.extractFromSource(any(), any(), any()))
                .thenReturn(new DefaultBlueprintCatalog().readingPassage1());
        when(blueprintExtractorService.toJson(any())).thenReturn("{}");
        when(repository.saveBlueprint(any())).thenReturn(bp);

        mvc.perform(post("/api/admin/practice-generator/blueprints/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "rawText": "This is raw text for extraction",
                            "passageTitle": "Climate Change Blueprint",
                            "targetBand": "7.5"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Climate Change Blueprint"));
    }

    @Test
    void createJobStartsJobAndReturnsAccepted() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID bpId = UUID.randomUUID();
        JobStatusResponse jobResponse = new JobStatusResponse(
                jobId, UUID.randomUUID(), Skill.READING, "GENERATING", GenerationState.GENERATING, null, Instant.now(), null
        );
        when(jobOrchestrator.startJob(any(), any())).thenReturn(jobResponse);

        mvc.perform(post("/api/admin/practice-generator/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "sourceId": "%s",
                            "blueprintId": "%s",
                            "skill": "READING"
                        }
                        """.formatted(sourceId, bpId)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("GENERATING"));
    }

    @Test
    void getJobStatusReturnsExistingJob() throws Exception {
        UUID jobId = UUID.randomUUID();
        PracticeGenerationJob job = new PracticeGenerationJob(
                jobId, UUID.randomUUID(), UUID.randomUUID(), Skill.READING, "READY_FOR_REVIEW",
                "gemini-1.5-flash", "v1.0", null, UUID.randomUUID(), Instant.now(), null, Instant.now()
        );
        when(repository.findJobById(jobId)).thenReturn(Optional.of(job));

        mvc.perform(get("/api/admin/practice-generator/jobs/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_REVIEW"));
    }
}
