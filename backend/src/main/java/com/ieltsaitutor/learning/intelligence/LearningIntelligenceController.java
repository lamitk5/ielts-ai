package com.ieltsaitutor.learning.intelligence;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/learning")
public class LearningIntelligenceController {
    private final LearningIntelligenceService service;
    public LearningIntelligenceController(LearningIntelligenceService service) { this.service = service; }

    @GetMapping("/profile") public StudentLearningProfile profile(@RequestAttribute(value = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal) { return service.profile(user(principal)); }
    @GetMapping("/skills") public List<StudentSkillProfile> skills(@RequestAttribute(value = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal) { return service.skills(user(principal)); }
    @GetMapping("/mistakes") public List<MistakeRecord> mistakes(@RequestParam(required = false) String skill, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit, @RequestAttribute(value = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal) {
        if (limit < 1 || limit > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be 1..100");
        return service.mistakes(user(principal)).stream().filter(item -> skill == null || item.skill().name().equalsIgnoreCase(skill))
                .filter(item -> status == null || item.status().name().equalsIgnoreCase(status)).limit(limit).toList();
    }
    @GetMapping("/issues") public List<StudentLearningIssue> issues(@RequestAttribute(value = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal) { return service.issues(user(principal)); }
    @GetMapping("/roadmap") public LearningRoadmap roadmap(@RequestAttribute(value = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal) { return service.roadmap(user(principal)); }
    @GetMapping("/activity") public List<LearningEvent> activity(@RequestAttribute(value = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal) { return service.activity(user(principal)); }
    @PostMapping("/roadmap/items/{itemId}/complete") public CompletionResponse complete(@PathVariable UUID itemId,
            @RequestAttribute(value = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal) {
        boolean completed = service.completeRoadmapItem(user(principal), itemId);
        if (!completed) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "roadmap item not found");
        return new CompletionResponse(true);
    }

    private UUID user(AuthPrincipal principal) { if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để xem tiến độ."); return principal.userId(); }
    public record CompletionResponse(boolean completed) {}
}
