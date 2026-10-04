package com.ieltsaitutor.writing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/practice/writing")
public class WritingController {
    private final WritingAssessmentService service;
    private final WritingRepository repository;
    private final WritingEvaluationService evaluationService;
    private final WritingVersionComparisonService comparisonService;

    public WritingController(
            WritingAssessmentService service,
            WritingRepository repository,
            WritingEvaluationService evaluationService,
            WritingVersionComparisonService comparisonService) {
        this.service = service;
        this.repository = repository;
        this.evaluationService = evaluationService;
        this.comparisonService = comparisonService;
    }

    @GetMapping("/tasks")
    public List<WritingTask> tasks() {
        return List.of(
                new WritingTask("task-1-academic-01", "TASK_1", "Academic Task 1", "Summarise the information in a chart or process.", 150),
                new WritingTask("task-2-opinion-01", "TASK_2", "Essay Task 2", "Discuss both views and give your own opinion.", 250));
    }

    @PostMapping("/submissions")
    public WritingAssessment submit(@RequestBody SubmissionRequest request, HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        if (request.responseText() == null || request.responseText().trim().split("\\s+").length < 20)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bài viết cần thêm nội dung để đánh giá.");
        return service.assess(principal.userId(), request.taskId(), request.responseText());
    }

    @GetMapping("/submissions")
    public List<WritingAssessment> submissions(HttpServletRequest request) {
        return repository.findByUser(principal(request).userId());
    }

    @PostMapping("/attempts")
    public WritingAttemptResponse startAttempt(@RequestBody AttemptStartRequest request, HttpServletRequest httpRequest) {
        AuthPrincipal authenticated = principal(httpRequest);
        return WritingAttemptResponse.from(service.startAttempt(authenticated.userId(), request.taskId()));
    }

    @GetMapping("/attempts/{attemptId}")
    public WritingAttemptResponse getAttempt(@PathVariable UUID attemptId, HttpServletRequest httpRequest) {
        return WritingAttemptResponse.from(service.getAttempt(principal(httpRequest).userId(), attemptId));
    }

    @PutMapping("/attempts/{attemptId}/draft")
    public WritingAttemptResponse saveAttemptDraft(@PathVariable UUID attemptId, @RequestBody DraftRequest request, HttpServletRequest httpRequest) {
        return WritingAttemptResponse.from(service.saveAttemptDraft(principal(httpRequest).userId(), attemptId, request.responseText()));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public WritingAttemptResponse submitAttempt(@PathVariable UUID attemptId, @RequestBody DraftRequest request, HttpServletRequest httpRequest) {
        return WritingAttemptResponse.from(service.submitAttempt(principal(httpRequest).userId(), attemptId, request.responseText()));
    }

    // Phase 4C Canonical & Versioning Endpoints
    @GetMapping("/submissions/{submissionId}/versions")
    public List<WritingSubmissionVersion> getVersions(@PathVariable UUID submissionId, HttpServletRequest request) {
        AuthPrincipal principal = principal(request);
        return evaluationService.getVersions(principal.userId(), submissionId);
    }

    @PostMapping("/submissions/{submissionId}/versions")
    public WritingSubmissionVersion createVersion(
            @PathVariable UUID submissionId,
            @RequestBody CreateVersionRequest req,
            HttpServletRequest request) {
        AuthPrincipal principal = principal(request);
        return evaluationService.createVersion(principal.userId(), submissionId, req.responseText(), req.parentVersionId());
    }

    @PostMapping("/submissions/{submissionId}/versions/{versionId}/evaluate")
    public WritingEvaluationResult evaluateVersion(
            @PathVariable UUID submissionId,
            @PathVariable UUID versionId,
            HttpServletRequest request) {
        AuthPrincipal principal = principal(request);
        return evaluationService.evaluateVersion(principal.userId(), submissionId, versionId);
    }

    @PostMapping("/submissions/{submissionId}/versions/{versionId}/retry")
    public WritingEvaluationResult retryEvaluation(
            @PathVariable UUID submissionId,
            @PathVariable UUID versionId,
            HttpServletRequest request) {
        AuthPrincipal principal = principal(request);
        return evaluationService.retryEvaluation(principal.userId(), submissionId, versionId);
    }

    @GetMapping("/submissions/{submissionId}/versions/{versionId}/evaluation")
    public WritingEvaluationResult getLatestEvaluation(
            @PathVariable UUID submissionId,
            @PathVariable UUID versionId,
            HttpServletRequest request) {
        AuthPrincipal principal = principal(request);
        return evaluationService.getLatestEvaluation(principal.userId(), submissionId, versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có đánh giá cho phiên bản này"));
    }

    @GetMapping("/submissions/{submissionId}/compare")
    public WritingVersionComparisonView compareVersions(
            @PathVariable UUID submissionId,
            @RequestParam int baseVersion,
            @RequestParam int targetVersion,
            HttpServletRequest request) {
        AuthPrincipal principal = principal(request);
        List<WritingSubmissionVersion> versions = evaluationService.getVersions(principal.userId(), submissionId);
        WritingSubmissionVersion base = versions.stream().filter(v -> v.versionNumber() == baseVersion).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Base version not found"));
        WritingSubmissionVersion target = versions.stream().filter(v -> v.versionNumber() == targetVersion).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target version not found"));

        WritingEvaluationResult baseEval = evaluationService.getLatestEvaluation(principal.userId(), submissionId, base.id()).orElse(null);
        WritingEvaluationResult targetEval = evaluationService.getLatestEvaluation(principal.userId(), submissionId, target.id()).orElse(null);

        return comparisonService.compare(base, baseEval, target, targetEval);
    }

    private AuthPrincipal principal(HttpServletRequest request) {
        Object principal = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (principal instanceof AuthPrincipal authenticated) return authenticated;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }

    public record SubmissionRequest(String taskId, String responseText) {}
    public record AttemptStartRequest(String taskId) {}
    public record DraftRequest(String responseText) {}
    public record CreateVersionRequest(String responseText, UUID parentVersionId) {}
    public record WritingAttemptResponse(UUID id, UUID userId, String taskId, String taskType, String status,
            String responseText, int wordCount, java.time.Instant createdAt, java.time.Instant submittedAt, WritingAssessment assessment) {
        static WritingAttemptResponse from(WritingAttempt attempt) {
            return new WritingAttemptResponse(attempt.id(), attempt.userId(), attempt.taskId(), attempt.taskType(), attempt.status(),
                    attempt.responseText(), attempt.wordCount(), attempt.createdAt(), attempt.submittedAt(), attempt.assessment());
        }
    }
}
