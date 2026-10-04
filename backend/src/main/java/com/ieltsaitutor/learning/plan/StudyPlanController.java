package com.ieltsaitutor.learning.plan;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.learning.intelligence.LearningIntelligenceService;
import com.ieltsaitutor.learning.vocabulary.VocabularyService;
import com.ieltsaitutor.onboarding.LearnerOnboardingService;
import com.ieltsaitutor.profile.LearnerProfileService;

@RestController
@RequestMapping("/api/study-plan")
public class StudyPlanController {
    private final LearnerProfileService profiles;
    private final LearningIntelligenceService intelligence;
    private final StudyPlanService plans;
    private final VocabularyService vocabulary;
    public StudyPlanController(LearnerProfileService profiles, LearningIntelligenceService intelligence,
            VocabularyService vocabulary) { this.profiles = profiles; this.intelligence = intelligence; this.vocabulary = vocabulary; this.plans = new StudyPlanService(); }
    @GetMapping public StudyPlan get(@RequestAttribute(value = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal) {
        UUID userId = user(principal);
        var profile = profiles.getProfile(userId);
        return plans.build(profile, intelligence.skills(userId),
                intelligence.activity(userId), vocabulary.list(userId, null, null).size(), LocalDate.now());
    }
    private UUID user(AuthPrincipal principal) { if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để xem lộ trình."); return principal.userId(); }
}
