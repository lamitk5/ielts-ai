package com.ieltsaitutor.practice.attempt;

import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/attempts")
public class AttemptController {
    private final AttemptService service;

    public AttemptController(AttemptService service) { this.service = service; }

    @PostMapping
    public AttemptView start(@RequestBody StartRequest request, HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        return view(service.start(principal.userId(), request.practiceId(), request.practiceVersion(), request.skill(), request.idempotencyKey()));
    }

    @GetMapping("/{attemptId}")
    public AttemptView get(@PathVariable UUID attemptId, HttpServletRequest httpRequest) {
        return view(service.get(principal(httpRequest).userId(), attemptId));
    }

    @PutMapping("/{attemptId}/answers")
    public AttemptView saveAnswers(@PathVariable UUID attemptId, @RequestBody AnswersRequest request, HttpServletRequest httpRequest) {
        return view(service.saveAnswers(principal(httpRequest).userId(), attemptId, request.answers()));
    }

    @PostMapping("/{attemptId}/submit")
    public AttemptView submit(@PathVariable UUID attemptId, @RequestBody SubmitRequest request, HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        return view(service.submit(principal.userId(), attemptId, request.answers(), request.idempotencyKey()));
    }

    @GetMapping("/{attemptId}/result")
    public AttemptView result(@PathVariable UUID attemptId, HttpServletRequest httpRequest) {
        return view(service.get(principal(httpRequest).userId(), attemptId));
    }

    private AuthPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (value instanceof AuthPrincipal principal) return principal;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }

    private AttemptView view(PracticeAttempt attempt) {
        return new AttemptView(attempt.id(), attempt.practiceId(), attempt.practiceVersion(), attempt.skill(), attempt.status().name(),
                attempt.answers(), attempt.score(), attempt.total(), attempt.startedAt(), attempt.submittedAt(), attempt.resultPayload());
    }

    public record StartRequest(String practiceId, String practiceVersion, String skill, String idempotencyKey) {}
    public record AnswersRequest(Map<String, String> answers) {}
    public record SubmitRequest(Map<String, String> answers, String idempotencyKey) {}
    public record AttemptView(UUID id, String practiceId, String practiceVersion, String skill, String status,
            Map<String, String> answers, Integer score, Integer total, java.time.Instant startedAt,
            java.time.Instant submittedAt, String resultPayload) {}
}
