package com.ieltsaitutor.assessment.objective;

import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.submission.CanonicalSubmissionService;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.SubmissionConflictException;
import com.ieltsaitutor.submission.SubmissionStatus;

@RestController
@RequestMapping("/api/submissions")
public class ObjectiveResultController {
    private final CanonicalSubmissionService submissions;
    private final QuestionResultRepository results;

    public ObjectiveResultController(CanonicalSubmissionService submissions, QuestionResultRepository results) {
        this.submissions = submissions;
        this.results = results;
    }

    @GetMapping("/{submissionId}/result")
    public ObjectiveResultView result(@PathVariable UUID submissionId, HttpServletRequest request) {
        AuthPrincipal principal = principal(request);
        PracticeSubmission submission = submissions.get(principal.userId(), submissionId);
        if (submission.status() != SubmissionStatus.GRADED) return ObjectiveResultView.from(submission, java.util.List.of());
        return ObjectiveResultView.from(submission, results.findByOwnedSubmission(principal.userId(), submissionId));
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
}
