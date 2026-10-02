package com.ieltsaitutor.speaking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/practice/speaking/submissions")
public class SpeakingReviewController {

    private final SpeakingReviewService reviewService;

    public SpeakingReviewController(SpeakingReviewService reviewService) {
        this.reviewService = reviewService;
    }

    public record SubmitReviewRequest(
            Double overallBand,
            Double fluencyCoherence,
            Double lexicalResource,
            Double grammaticalRange,
            Double pronunciation,
            String reviewerFeedback,
            String criteriaJson
    ) {}

    public record SpeakingReviewResponse(
            UUID id,
            UUID submissionId,
            UUID reviewerUserId,
            int reviewVersion,
            Double overallBand,
            Double fluencyCoherence,
            Double lexicalResource,
            Double grammaticalRange,
            Double pronunciation,
            String reviewerFeedback,
            String criteriaJson,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static SpeakingReviewResponse from(SpeakingReview r) {
            return new SpeakingReviewResponse(
                    r.id(), r.submissionId(), r.reviewerUserId(), r.reviewVersion(),
                    r.overallBand(), r.fluencyCoherence(), r.lexicalResource(),
                    r.grammaticalRange(), r.pronunciation(), r.reviewerFeedback(),
                    r.criteriaJson(), r.status(), r.createdAt(), r.updatedAt()
            );
        }
    }

    @PostMapping("/{submissionId}/review")
    public SpeakingReviewResponse submitReview(
            @PathVariable UUID submissionId,
            @RequestBody SubmitReviewRequest request,
            HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        try {
            SpeakingReview review = reviewService.submitReview(
                    principal,
                    submissionId,
                    request.overallBand(),
                    request.fluencyCoherence(),
                    request.lexicalResource(),
                    request.grammaticalRange(),
                    request.pronunciation(),
                    request.reviewerFeedback(),
                    request.criteriaJson()
            );
            return SpeakingReviewResponse.from(review);
        } catch (SecurityException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/{submissionId}/review")
    public SpeakingReviewResponse getLatestReview(
            @PathVariable UUID submissionId,
            HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        try {
            return reviewService.getLatestReview(principal, submissionId)
                    .map(SpeakingReviewResponse::from)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có đánh giá cho bài nộp"));
        } catch (SecurityException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/{submissionId}/reviews")
    public List<SpeakingReviewResponse> getReviewHistory(
            @PathVariable UUID submissionId,
            HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        try {
            return reviewService.getReviewHistory(principal, submissionId)
                    .stream()
                    .map(SpeakingReviewResponse::from)
                    .toList();
        } catch (SecurityException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    private AuthPrincipal principal(HttpServletRequest request) {
        Object principal = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (principal instanceof AuthPrincipal authenticated) return authenticated;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }
}
