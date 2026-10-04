package com.ieltsaitutor.results;

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
@RequestMapping("/api")
public class SubmissionReviewController {
    private final SubmissionReviewService service;

    public SubmissionReviewController(SubmissionReviewService service) { this.service = service; }

    public record ReviewRequest(Double overallBand, Double fluencyCoherence, Double lexicalResource,
            Double grammaticalRange, Double pronunciation, String reviewerFeedback, String criteriaJson) {}

    public record ReviewResponse(UUID id, UUID submissionId, UUID reviewerUserId, int reviewVersion,
            Double overallBand, Double fluencyCoherence, Double lexicalResource, Double grammaticalRange,
            Double pronunciation, String reviewerFeedback, String criteriaJson, String status,
            Instant createdAt, Instant updatedAt) {
        static ReviewResponse from(SubmissionReview review) {
            return new ReviewResponse(review.id(), review.submissionId(), review.reviewerUserId(), review.reviewVersion(),
                    review.overallBand(), review.fluencyCoherence(), review.lexicalResource(), review.grammaticalRange(),
                    review.pronunciation(), review.reviewerFeedback(), review.criteriaJson(), review.status(),
                    review.createdAt(), review.updatedAt());
        }
    }

    @PostMapping("/admin/submissions/{submissionId}/review")
    public ReviewResponse submit(@PathVariable UUID submissionId, @RequestBody ReviewRequest request, HttpServletRequest http) {
        try {
            SubmissionReview review = service.submit(principal(http), submissionId, request.overallBand(), request.fluencyCoherence(),
                    request.lexicalResource(), request.grammaticalRange(), request.pronunciation(), request.reviewerFeedback(), request.criteriaJson());
            return ReviewResponse.from(review);
        } catch (SecurityException ex) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage()); }
        catch (RuntimeException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()); }
    }

    @GetMapping("/results/submissions/{submissionId}/reviews")
    public List<ReviewResponse> history(@PathVariable UUID submissionId, HttpServletRequest http) {
        try { return service.historyForLearner(principal(http), submissionId).stream().map(ReviewResponse::from).toList(); }
        catch (SecurityException ex) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage()); }
        catch (RuntimeException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage()); }
    }

    private AuthPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (value instanceof AuthPrincipal principal) return principal;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }
}
