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
import com.ieltsaitutor.practice.generator.dto.ReviewActionRequest;
import com.ieltsaitutor.practice.generator.dto.ReviewActionResponse;
import com.ieltsaitutor.practice.generator.service.PracticeReviewService;

@RestController
@RequestMapping("/api/admin/practice-generator")
public class AdminPracticeReviewController {

    private final PracticeReviewService reviewService;

    public AdminPracticeReviewController(PracticeReviewService reviewService) {
        this.reviewService = reviewService;
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

    @GetMapping("/sets/{setId}/versions")
    public ResponseEntity<List<GeneratedPracticeVersion>> getVersionHistory(@PathVariable UUID setId) {
        return ResponseEntity.ok(reviewService.getVersionHistory(setId));
    }

    @GetMapping("/sets/{setId}/audit")
    public ResponseEntity<List<PracticeReviewAction>> getAuditHistory(@PathVariable UUID setId) {
        return ResponseEntity.ok(reviewService.getAuditHistory(setId));
    }
}
