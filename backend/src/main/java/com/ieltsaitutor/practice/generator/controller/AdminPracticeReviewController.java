package com.ieltsaitutor.practice.generator.controller;

import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeSet;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeVersion;
import com.ieltsaitutor.practice.generator.domain.PracticeReviewAction;
import com.ieltsaitutor.practice.generator.dto.GeneratedSetReviewPayload;
import com.ieltsaitutor.practice.generator.dto.ManualEditSetRequest;
import com.ieltsaitutor.practice.generator.dto.RegenerateItemRequest;
import com.ieltsaitutor.practice.generator.dto.ReviewActionRequest;
import com.ieltsaitutor.practice.generator.dto.ReviewActionResponse;
import com.ieltsaitutor.practice.generator.dto.VersionComparisonDto;
import com.ieltsaitutor.practice.generator.service.PracticeReviewService;
import com.ieltsaitutor.practice.generator.service.PracticeRevisionService;

@RestController
@RequestMapping("/api/admin/practice-generator")
public class AdminPracticeReviewController {

    private final PracticeReviewService reviewService;
    private final PracticeRevisionService revisionService;

    public AdminPracticeReviewController(PracticeReviewService reviewService, PracticeRevisionService revisionService) {
        this.reviewService = reviewService;
        this.revisionService = revisionService;
    }

    @GetMapping("/sets")
    public ResponseEntity<List<GeneratedPracticeSet>> listSets(
            @RequestParam(required = false) String state) {
        return ResponseEntity.ok(reviewService.listSets(state));
    }

    @GetMapping("/sets/{setId}")
    public ResponseEntity<GeneratedSetReviewPayload> getReviewPayload(@PathVariable UUID setId) {
        return ResponseEntity.ok(reviewService.getReviewPayload(setId));
    }

    @PostMapping("/sets/{setId}/review")
    public ResponseEntity<ReviewActionResponse> executeReview(
            @PathVariable UUID setId,
            @Valid @RequestBody ReviewActionRequest request,
            HttpServletRequest servletRequest) {
        Object principal = servletRequest.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        UUID adminId = (principal instanceof AuthPrincipal auth) ? auth.userId() : UUID.randomUUID();
        ReviewActionResponse response = reviewService.executeReview(setId, request, adminId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sets/{setId}/items/regenerate")
    public ResponseEntity<GeneratedPracticeVersion> regenerateItem(
            @PathVariable UUID setId,
            @Valid @RequestBody RegenerateItemRequest request,
            HttpServletRequest servletRequest) {
        Object principal = servletRequest.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        UUID adminId = (principal instanceof AuthPrincipal auth) ? auth.userId() : UUID.randomUUID();
        GeneratedPracticeVersion version = revisionService.regenerateItem(setId, request, adminId);
        return ResponseEntity.ok(version);
    }

    @PostMapping("/sets/{setId}/edit")
    public ResponseEntity<GeneratedPracticeVersion> applyManualEdit(
            @PathVariable UUID setId,
            @Valid @RequestBody ManualEditSetRequest request,
            HttpServletRequest servletRequest) {
        Object principal = servletRequest.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        UUID adminId = (principal instanceof AuthPrincipal auth) ? auth.userId() : UUID.randomUUID();
        GeneratedPracticeVersion version = revisionService.applyManualEdit(setId, request, adminId);
        return ResponseEntity.ok(version);
    }

    @GetMapping("/sets/{setId}/compare")
    public ResponseEntity<VersionComparisonDto> compareVersions(
            @PathVariable UUID setId,
            @RequestParam int v1,
            @RequestParam int v2) {
        return ResponseEntity.ok(revisionService.compareVersions(setId, v1, v2));
    }

    @GetMapping("/sets/{setId}/versions")
    public ResponseEntity<List<GeneratedPracticeVersion>> getVersionHistory(@PathVariable UUID setId) {
        return ResponseEntity.ok(reviewService.getVersionHistory(setId));
    }

    @GetMapping("/sets/{setId}/audit")
    public ResponseEntity<List<PracticeReviewAction>> getAuditHistory(@PathVariable UUID setId) {
        return ResponseEntity.ok(reviewService.getAuditHistory(setId));
    }
}
