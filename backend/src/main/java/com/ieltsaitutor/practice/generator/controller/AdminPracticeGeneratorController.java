package com.ieltsaitutor.practice.generator.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintExtractorService;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.blueprint.DefaultBlueprintCatalog;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationBlueprint;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationJob;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.dto.CreateJobRequest;
import com.ieltsaitutor.practice.generator.dto.ExtractBlueprintRequest;
import com.ieltsaitutor.practice.generator.dto.JobStatusResponse;
import com.ieltsaitutor.practice.generator.dto.RegisterSourceRequest;
import com.ieltsaitutor.practice.generator.dto.RegisterSourceResponse;
import com.ieltsaitutor.practice.generator.service.GenerationJobOrchestrator;
import com.ieltsaitutor.practice.generator.repository.PracticeGenerationRepository;
import com.ieltsaitutor.practice.generator.service.SourceNormalizationService;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.domain.Skill;

@RestController
@RequestMapping("/api/admin/practice-generator")
public class AdminPracticeGeneratorController {

    private final GenerationJobOrchestrator jobOrchestrator;
    private final PracticeGenerationRepository repository;
    private final SourceNormalizationService normalizationService;
    private final BlueprintExtractorService blueprintExtractorService;

    public AdminPracticeGeneratorController(
            GenerationJobOrchestrator jobOrchestrator,
            PracticeGenerationRepository repository,
            SourceNormalizationService normalizationService,
            BlueprintExtractorService blueprintExtractorService) {
        this.jobOrchestrator = jobOrchestrator;
        this.repository = repository;
        this.normalizationService = normalizationService;
        this.blueprintExtractorService = blueprintExtractorService;
    }

    @PostMapping("/sources")
    public ResponseEntity<RegisterSourceResponse> registerSource(
            @Valid @RequestBody RegisterSourceRequest request,
            HttpServletRequest servletRequest) {
        Object principal = servletRequest.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        UUID createdBy = (principal instanceof AuthPrincipal auth) ? auth.userId() : UUID.randomUUID();

        String normalizedText = normalizationService.normalizeText(request.rawText());
        Skill skill = request.skill() != null ? Skill.valueOf(request.skill().toUpperCase()) : Skill.READING;
        normalizationService.validateWordCount(normalizedText, skill);
        String checksum = normalizationService.computeChecksum(normalizedText);
        int wordCount = normalizationService.countWords(normalizedText);

        PracticeGenerationSource source = new PracticeGenerationSource(
                UUID.randomUUID(),
                request.title(),
                "TEXT",
                "ADMIN",
                request.rightsStatus(),
                "Admin uploaded source",
                normalizedText,
                checksum,
                createdBy,
                Instant.now(),
                Instant.now()
        );
        PracticeGenerationSource saved = repository.saveSource(source);
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterSourceResponse(
                saved.id(),
                saved.title(),
                saved.rightsStatus(),
                wordCount,
                saved.checksum(),
                saved.createdAt()
        ));
    }

    @GetMapping("/sources")
    public ResponseEntity<List<PracticeGenerationSource>> listSources(
            @RequestParam(required = false) RightsStatus rightsStatus) {
        return ResponseEntity.ok(repository.listSources(rightsStatus));
    }

    @PostMapping("/blueprints/extract")
    public ResponseEntity<PracticeGenerationBlueprint> extractBlueprint(
            @Valid @RequestBody ExtractBlueprintRequest request,
            HttpServletRequest servletRequest) {
        Object principal = servletRequest.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        UUID createdBy = (principal instanceof AuthPrincipal auth) ? auth.userId() : UUID.randomUUID();

        PracticeGenerationSource source;
        if (request.sourceId() != null) {
            source = repository.findSourceById(request.sourceId()).orElse(null);
            if (source == null) {
                return ResponseEntity.notFound().build();
            }
        } else if (request.rawText() != null && !request.rawText().isBlank()) {
            String clean = normalizationService.normalizeText(request.rawText());
            source = new PracticeGenerationSource(
                    UUID.randomUUID(),
                    request.passageTitle() != null ? request.passageTitle() : "Ad-hoc source",
                    "TEXT", "ADMIN", RightsStatus.APPROVED, "",
                    clean, normalizationService.computeChecksum(clean),
                    createdBy, Instant.now(), Instant.now()
            );
        } else {
            return ResponseEntity.badRequest().build();
        }

        BigDecimal targetBand = request.targetBand() != null ? new BigDecimal(request.targetBand()) : BigDecimal.valueOf(7.5);
        BlueprintSchema schema = blueprintExtractorService.extractFromSource(source, Skill.READING, targetBand);
        String schemaJson = blueprintExtractorService.toJson(schema);

        PracticeGenerationBlueprint blueprint = new PracticeGenerationBlueprint(
                UUID.randomUUID(),
                Skill.READING,
                request.passageTitle() != null ? request.passageTitle() : (source.title() + " Blueprint"),
                targetBand,
                schemaJson,
                createdBy,
                Instant.now(),
                Instant.now()
        );
        PracticeGenerationBlueprint saved = repository.saveBlueprint(blueprint);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/blueprints")
    public ResponseEntity<List<PracticeGenerationBlueprint>> listBlueprints(
            @RequestParam(required = false) String skill) {
        return ResponseEntity.ok(repository.listBlueprints(skill));
    }

    @PostMapping("/jobs")
    public ResponseEntity<JobStatusResponse> createJob(
            @Valid @RequestBody CreateJobRequest request,
            HttpServletRequest servletRequest) {
        Object principal = servletRequest.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        UUID adminId = (principal instanceof AuthPrincipal auth) ? auth.userId() : UUID.randomUUID();

        JobStatusResponse response = jobOrchestrator.startJob(request, adminId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<PracticeGenerationJob>> listJobs(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(repository.listJobs(limit, offset));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable UUID jobId) {
        return repository.findJobById(jobId)
                .map(job -> new JobStatusResponse(
                        job.id(),
                        null,
                        job.skill(),
                        job.status(),
                        null,
                        job.errorMessage(),
                        job.createdAt(),
                        job.completedAt()
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
