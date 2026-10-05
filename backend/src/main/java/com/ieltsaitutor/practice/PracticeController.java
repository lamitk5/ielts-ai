package com.ieltsaitutor.practice;

import java.util.List;
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
import com.ieltsaitutor.practice.attempt.PracticeAttempt;

@RestController
@RequestMapping("/api/practice")
public class PracticeController {
    private final PracticeService service;

    public PracticeController(PracticeService service) { this.service = service; }

    @GetMapping("/{skill}/sets")
    public List<PracticeSetView> sets(@PathVariable String skill) {
        try {
            return service.sets(skill).stream().map(PracticeSetView::from).toList();
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
    }

    @GetMapping("/{skill}/sets/{id}")
    public PracticeSetView set(@PathVariable String skill, @PathVariable String id) { return PracticeSetView.from(service.set(skill, id)); }

    @PostMapping("/{skill}/attempts")
    public AttemptResponse submit(@PathVariable String skill, @RequestBody AttemptRequest request, HttpServletRequest httpRequest) {
        Object principal = httpRequest.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (!(principal instanceof AuthPrincipal authenticated)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
        PracticeAttemptResult result = service.submit(skill, request.setId(), request.answers(), authenticated.userId());
        return new AttemptResponse(result.attemptId(), result.score(), result.total());
    }

    @PostMapping("/{skill}/attempts/start")
    public ReadingAttemptResponse start(@PathVariable String skill, @RequestBody StartRequest request, HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        ensureObjectiveSkill(skill);
        PracticeAttempt attempt = "reading".equalsIgnoreCase(skill)
                ? service.startReadingAttempt(request.setId(), principal.userId(), request.idempotencyKey())
                : service.startListeningAttempt(request.setId(), principal.userId(), request.idempotencyKey());
        return ReadingAttemptResponse.from(attempt);
    }

    @PutMapping("/{skill}/attempts/{attemptId}/answers")
    public ReadingAttemptResponse saveAnswers(@PathVariable String skill, @PathVariable UUID attemptId,
            @RequestBody AnswersRequest request, HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        ensureObjectiveSkill(skill);
        return ReadingAttemptResponse.from(service.saveObjectiveAnswers(principal.userId(), attemptId, request.answers()));
    }

    @PostMapping("/{skill}/attempts/{attemptId}/submit")
    public ReadingAttemptResponse submitDurable(@PathVariable String skill, @PathVariable UUID attemptId,
            @RequestBody DurableSubmitRequest request, HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        ensureObjectiveSkill(skill);
        PracticeAttempt attempt = "reading".equalsIgnoreCase(skill)
                ? service.submitReadingAttempt(principal.userId(), attemptId, request.answers(), request.idempotencyKey())
                : service.submitListeningAttempt(principal.userId(), attemptId, request.answers(), request.idempotencyKey());
        return ReadingAttemptResponse.from(attempt);
    }

    @GetMapping("/{skill}/attempts/{attemptId}/result")
    public ReadingAttemptResponse result(@PathVariable String skill, @PathVariable UUID attemptId, HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        ensureObjectiveSkill(skill);
        PracticeAttempt attempt = "reading".equalsIgnoreCase(skill)
                ? service.readingResult(principal.userId(), attemptId)
                : service.listeningResult(principal.userId(), attemptId);
        return ReadingAttemptResponse.from(attempt);
    }

    private void ensureObjectiveSkill(String skill) {
        if (!"reading".equalsIgnoreCase(skill) && !"listening".equalsIgnoreCase(skill)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Durable flow is only available for Reading and Listening.");
        }
    }

    private AuthPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (value instanceof AuthPrincipal principal) return principal;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }

    public record AttemptRequest(String setId, Map<String, String> answers) {}
    public record AttemptResponse(UUID attemptId, int score, int total) {}
    public record StartRequest(String setId, String idempotencyKey) {}
    public record AnswersRequest(Map<String, String> answers) {}
    public record DurableSubmitRequest(Map<String, String> answers, String idempotencyKey) {}
    public record ReadingAttemptResponse(UUID id, String practiceId, String practiceVersion, String skill, String status,
            Map<String, String> answers, Integer score, Integer total, java.time.Instant startedAt,
            java.time.Instant submittedAt, String resultPayload) {
        static ReadingAttemptResponse from(PracticeAttempt attempt) {
            return new ReadingAttemptResponse(attempt.id(), attempt.practiceId(), attempt.practiceVersion(), attempt.skill(),
                    attempt.status().name(), attempt.answers(), attempt.score(), attempt.total(), attempt.startedAt(),
                    attempt.submittedAt(), attempt.resultPayload());
        }
    }

    public record PracticeSetView(String id, String skill, String title, String description, List<QuestionView> questions, PassageView passage) {
        static PracticeSetView from(PracticeSet set) { return new PracticeSetView(set.id(), set.skill(), set.title(), set.description(),
                set.questions().stream().map(question -> new QuestionView(question.id(), question.prompt(), question.options())).toList(),
                set.passage() == null ? null : new PassageView(set.passage().title(),
                        set.passage().paragraphs().stream().map(paragraph -> new ParagraphView(paragraph.id(), paragraph.text())).toList())); }
    }

    public record QuestionView(String id, String prompt, List<String> options) {}
    public record PassageView(String title, List<ParagraphView> paragraphs) {}
    public record ParagraphView(String id, String text) {}
}
