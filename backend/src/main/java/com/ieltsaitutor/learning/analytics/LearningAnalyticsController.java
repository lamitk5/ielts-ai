package com.ieltsaitutor.learning.analytics;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.learning.intelligence.LearningIntelligenceService;

@RestController
@RequestMapping("/api/analytics")
public class LearningAnalyticsController {
    private final LearningIntelligenceService intelligence;
    private final LearningAnalyticsService analytics = new LearningAnalyticsService();
    public LearningAnalyticsController(LearningIntelligenceService intelligence) { this.intelligence = intelligence; }
    @GetMapping public LearningAnalytics get(@RequestAttribute(value = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal) {
        UUID userId = user(principal);
        return analytics.build(userId, intelligence.activity(userId), intelligence.mistakes(userId), intelligence.skills(userId));
    }
    private UUID user(AuthPrincipal principal) { if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để xem phân tích."); return principal.userId(); }
}
