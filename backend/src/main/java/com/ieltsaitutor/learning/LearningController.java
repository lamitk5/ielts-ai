package com.ieltsaitutor.learning;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/me")
public class LearningController {
    private final LearningProgressService service;

    public LearningController(LearningProgressService service) { this.service = service; }

    @GetMapping("/progress")
    public MemberProgress progress(HttpServletRequest request) { return service.progress(userId(request)); }

    @GetMapping("/activity")
    public List<LearningActivity> activity(HttpServletRequest request) { return service.activities(userId(request)); }

    @GetMapping("/attempts")
    public List<LearningAttempt> attempts(HttpServletRequest request) { return service.attempts(userId(request)); }

    @PostMapping("/attempts")
    public void record(@RequestBody AttemptRequest request, HttpServletRequest httpRequest) {
        UUID userId = userId(httpRequest);
        service.recordAttempt(userId, new LearningAttempt(UUID.randomUUID(), userId, request.skill(), request.score(),
                request.total(), Instant.now()));
    }

    private UUID userId(HttpServletRequest request) {
        Object principal = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (principal instanceof AuthPrincipal authenticated) return authenticated.userId();
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }

    public record AttemptRequest(String skill, int score, int total) {}
}
