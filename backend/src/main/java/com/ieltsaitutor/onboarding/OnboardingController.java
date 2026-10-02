package com.ieltsaitutor.onboarding;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthException;
import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/me/onboarding")
public class OnboardingController {
    private final LearnerOnboardingService service;

    public OnboardingController(LearnerOnboardingService service) { this.service = service; }

    @GetMapping
    public LearnerOnboardingProfile get(HttpServletRequest request) {
        return service.get(principal(request).userId());
    }

    @PutMapping
    public LearnerOnboardingProfile save(HttpServletRequest request, @RequestBody OnboardingGoalRequest body) {
        UUID userId = principal(request).userId();
        if (body == null || body.version() == null) {
            throw new AuthException("ONBOARDING_INVALID_REQUEST", HttpStatus.BAD_REQUEST,
                    "Thông tin mục tiêu chưa hợp lệ.");
        }
        return service.save(userId, body.toCommand(), body.version());
    }

    @PostMapping("/complete")
    public LearnerOnboardingProfile complete(HttpServletRequest request, @RequestBody OnboardingCompleteRequest body) {
        UUID userId = principal(request).userId();
        if (body == null || body.state() == null || body.version() == null) {
            throw new AuthException("ONBOARDING_INVALID_REQUEST", HttpStatus.BAD_REQUEST,
                    "Thông tin mục tiêu chưa hợp lệ.");
        }
        return service.complete(userId, body.state(), body.version());
    }
    private AuthPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (value instanceof AuthPrincipal principal) {
            return principal;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }

    /**
     * Only learner-editable goal fields are accepted. Any client-supplied owner,
     * measured level, evidence reference or source is discarded here by
     * construction, so self-report can never be promoted into measured evidence.
     */
    public record OnboardingGoalRequest(String selfReportedLevel, Double targetBand,
            java.time.LocalDate targetExamDate, com.ieltsaitutor.learning.intelligence.Skill perceivedWeakestSkill,
            Integer dailyStudyMinutes, Integer studyDaysPerWeek, OnboardingState state, Long version,
            String userId, String measuredLevel, String evidenceReference, String source) {

        OnboardingGoalCommand toCommand() {
            return new OnboardingGoalCommand(selfReportedLevel, targetBand, targetExamDate,
                    perceivedWeakestSkill, dailyStudyMinutes, studyDaysPerWeek,
                    state == null ? OnboardingState.IN_PROGRESS : state);
        }
    }

    public record OnboardingCompleteRequest(OnboardingState state, Long version) {
    }
}