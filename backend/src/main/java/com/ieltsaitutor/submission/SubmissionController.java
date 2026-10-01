package com.ieltsaitutor.submission;

import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {
    private final CanonicalSubmissionService service;

    public SubmissionController(CanonicalSubmissionService service) {
        this.service = service;
    }

    @PostMapping
    public SubmissionView start(@RequestBody StartRequest request, HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        return SubmissionView.from(service.start(principal.userId(),
                new SubmissionStartCommand(request.publishedSetId(), request.skill(), request.idempotencyKey())));
    }

    @GetMapping("/{submissionId}")
    public SubmissionView get(@PathVariable UUID submissionId, HttpServletRequest httpRequest) {
        return SubmissionView.from(service.get(principal(httpRequest).userId(), submissionId));
    }

    @PutMapping("/{submissionId}/draft")
    public DraftResponse draft(@PathVariable UUID submissionId, @RequestBody DraftRequest request,
            HttpServletRequest httpRequest) {
        UUID ownerId = principal(httpRequest).userId();
        SubmissionDraftSnapshot snapshot = service.autosave(ownerId, submissionId, request.payload(),
                request.expectedRevision(), request.idempotencyKey());
        return new DraftResponse(SubmissionView.from(service.get(ownerId, submissionId)), snapshot);
    }

    @PostMapping("/{submissionId}/submit")
    public SubmissionView submit(@PathVariable UUID submissionId, @RequestBody SubmitRequest request,
            HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        return SubmissionView.from(service.submit(principal.userId(), submissionId, request.payload(), request.idempotencyKey()));
    }

    @GetMapping("/{submissionId}/result")
    public SubmissionView result(@PathVariable UUID submissionId, HttpServletRequest httpRequest) {
        return SubmissionView.from(service.get(principal(httpRequest).userId(), submissionId));
    }

    @ExceptionHandler(SubmissionConflictException.class)
    public ResponseEntity<Map<String, String>> conflict(SubmissionConflictException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "SUBMISSION_CONFLICT", "message", error.getMessage()));
    }

    private AuthPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (value instanceof AuthPrincipal principal) return principal;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }

    public record StartRequest(String publishedSetId, String skill, String idempotencyKey) {}
    public record DraftRequest(Map<String, String> payload, long expectedRevision, String idempotencyKey) {}
    public record SubmitRequest(Map<String, String> payload, String idempotencyKey) {}
    public record DraftResponse(SubmissionView submission, SubmissionDraftSnapshot draft) {}
}
