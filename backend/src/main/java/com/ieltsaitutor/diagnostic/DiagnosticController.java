package com.ieltsaitutor.diagnostic;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.learning.intelligence.Skill;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/me/diagnostic")
public class DiagnosticController {
    private final DiagnosticSessionService sessions;
    private final DiagnosticSubmissionService submissions;
    private final DiagnosticAggregationService aggregation;

    public DiagnosticController(DiagnosticSessionService sessions, DiagnosticSubmissionService submissions, DiagnosticAggregationService aggregation) {
        this.sessions = sessions; this.submissions = submissions; this.aggregation = aggregation;
    }

    @GetMapping("/session") public DiagnosticSession session(HttpServletRequest req) { return sessions.start(principal(req).userId()); }
    @GetMapping("/history") public List<DiagnosticSession> history(HttpServletRequest req) { return sessions.history(principal(req).userId(), DiagnosticSessionService.MAX_HISTORY); }
    @PostMapping("/session/{id}/retake") public DiagnosticSession retake(@PathVariable UUID id, HttpServletRequest req) { return sessions.retake(principal(req).userId()); }
    @GetMapping("/{id}/result") public DiagnosticResult result(@PathVariable UUID id, HttpServletRequest req) { return aggregation.result(principal(req).userId(), id); }

    @PostMapping("/{id}/sections/{skill}/canonical/{submissionId}")
    public DiagnosticSectionResult canonical(@PathVariable UUID id, @PathVariable Skill skill, @PathVariable UUID submissionId, HttpServletRequest req) {
        return submissions.recordCanonicalResult(principal(req).userId(), id, skill, submissionId);
    }

    @PostMapping("/{id}/sections/{skill}/unavailable")
    public DiagnosticSectionResult unavailable(@PathVariable UUID id, @PathVariable Skill skill, @RequestBody(required = false) Message body, HttpServletRequest req) {
        return submissions.recordUnavailable(principal(req).userId(), id, skill, body == null ? "Phần này hiện chưa khả dụng." : body.message());
    }

    @PostMapping("/{id}/finish") public DiagnosticSession finish(@PathVariable UUID id, @RequestBody FinishRequest body, HttpServletRequest req) {
        return body != null && body.complete() ? sessions.submit(principal(req).userId(), id) : sessions.skip(principal(req).userId(), id);
    }
    public record FinishRequest(boolean complete) {}
    public record Message(String message) {}
    private AuthPrincipal principal(HttpServletRequest req) { Object value = req.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE); if (value instanceof AuthPrincipal p) return p; throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục."); }
}
