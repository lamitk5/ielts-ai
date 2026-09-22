package com.ieltsaitutor.practice;

import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/practice")
public class PracticeController {
    private final PracticeService service;

    public PracticeController(PracticeService service) { this.service = service; }

    @GetMapping("/{skill}/sets")
    public List<PracticeSetView> sets(@PathVariable String skill) {
        return service.sets(skill).stream().map(PracticeSetView::from).toList();
    }

    @GetMapping("/{skill}/sets/{id}")
    public PracticeSetView set(@PathVariable String skill, @PathVariable String id) { return PracticeSetView.from(service.set(skill, id)); }

    @PostMapping("/{skill}/attempts")
    public PracticeAttemptResult submit(@PathVariable String skill, @RequestBody AttemptRequest request, HttpServletRequest httpRequest) {
        Object principal = httpRequest.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (!(principal instanceof AuthPrincipal authenticated)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
        return service.submit(skill, request.setId(), request.answers(), authenticated.userId());
    }

    public record AttemptRequest(String setId, Map<String, String> answers) {}

    public record PracticeSetView(String id, String skill, String title, String description, List<QuestionView> questions) {
        static PracticeSetView from(PracticeSet set) { return new PracticeSetView(set.id(), set.skill(), set.title(), set.description(),
                set.questions().stream().map(question -> new QuestionView(question.id(), question.prompt(), question.options())).toList()); }
    }

    public record QuestionView(String id, String prompt, List<String> options) {}
}
