package com.ieltsaitutor.speaking;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/practice/speaking")
public class SpeakingController {
    private final SpeakingService service;
    public SpeakingController(SpeakingService service) { this.service = service; }

    @GetMapping("/prompts")
    public List<SpeakingPrompt> prompts() { return service.prompts(); }

    @PostMapping("/attempts")
    public SpeakingAttempt submit(@RequestBody AttemptRequest request, HttpServletRequest httpRequest) {
        return service.record(userId(httpRequest), request.promptId(), request.transcript(), request.audioFilename());
    }

    @GetMapping("/attempts")
    public List<SpeakingAttempt> attempts(HttpServletRequest request) { return service.attempts(userId(request)); }

    @PostMapping("/attempts/start")
    public SpeakingAttempt start(@RequestBody AttemptRequest request, HttpServletRequest httpRequest) {
        return service.startAttempt(userId(httpRequest), request.promptId());
    }

    @GetMapping("/attempts/{attemptId}")
    public SpeakingAttempt get(@PathVariable java.util.UUID attemptId, HttpServletRequest request) {
        return service.getAttempt(userId(request), attemptId);
    }

    @PutMapping("/attempts/{attemptId}/draft")
    public SpeakingAttempt draft(@PathVariable java.util.UUID attemptId, @RequestBody DraftRequest request, HttpServletRequest httpRequest) {
        return service.saveAttemptDraft(userId(httpRequest), attemptId, request.transcript());
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public SpeakingAttempt submitAttempt(@PathVariable java.util.UUID attemptId, @RequestBody DraftRequest request, HttpServletRequest httpRequest) {
        return service.submitAttempt(userId(httpRequest), attemptId, request.transcript());
    }

    private java.util.UUID userId(HttpServletRequest request) {
        Object principal = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (principal instanceof AuthPrincipal authenticated) return authenticated.userId();
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }

    public record AttemptRequest(String promptId, String transcript, String audioFilename) {}
    public record DraftRequest(String transcript) {}
}
