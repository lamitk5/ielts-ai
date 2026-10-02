package com.ieltsaitutor.onboarding;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.learning.intelligence.Skill;
import com.ieltsaitutor.onboarding.OnboardingState;

class OnboardingControllerSecurityTest {

    @Test
    void readRequiresAuthenticationAndUsesTheSessionOwner() throws Exception {
        UUID owner = UUID.randomUUID();
        LearnerOnboardingService service = mock(LearnerOnboardingService.class);
        when(service.get(owner)).thenReturn(LearnerOnboardingProfile.empty(owner, Instant.now()));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new OnboardingController(service)).build();

        mvc.perform(get("/api/me/onboarding").requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                        new AuthPrincipal(owner, "owner@example.com", "Owner", com.ieltsaitutor.auth.UserRole.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(owner.toString()))
                .andExpect(jsonPath("$.state").value(OnboardingState.NOT_STARTED.name()))
                .andExpect(jsonPath("$.source").value("SELF_REPORTED"));
        mvc.perform(get("/api/me/onboarding")).andExpect(status().isUnauthorized());
        verify(service).get(eq(owner));
    }

    @Test
    void saveIgnoresAnyClientSuppliedOwnerAndNeverEchoesAnotherUser() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        LearnerOnboardingService service = mock(LearnerOnboardingService.class);
        when(service.save(eq(owner), any(OnboardingGoalCommand.class), eq(0L)))
                .thenReturn(goal(owner));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new OnboardingController(service)).build();

        mvc.perform(put("/api/me/onboarding").contentType(MediaType.APPLICATION_JSON).content("""
                {"selfReportedLevel":"BEGINNER","targetBand":5.0,"perceivedWeakestSkill":"READING",
                 "dailyStudyMinutes":30,"studyDaysPerWeek":3,"state":"COMPLETED","version":0,
                 "userId":"00000000-0000-0000-0000-000000000099","measuredLevel":"ADVANCED",
                 "evidenceReference":"client-fabricated","source":"MEASURED_EVIDENCE"}
                """).requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                        new AuthPrincipal(owner, "owner@example.com", "Owner", com.ieltsaitutor.auth.UserRole.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(owner.toString()))
                .andExpect(jsonPath("$.source").value("SELF_REPORTED"))
                .andExpect(jsonPath("$.measuredLevel").doesNotExist())
                .andExpect(jsonPath("$.evidenceReference").doesNotExist());

        verify(service).save(eq(owner), eq(new OnboardingGoalCommand("BEGINNER", 5.0, null, Skill.READING,
                30, 3, OnboardingState.COMPLETED)), eq(0L));
        verify(service, never()).save(eq(attacker), any(OnboardingGoalCommand.class), eq(0L));
    }

    @Test
    void completeIsOwnerScopedAndIdempotent() throws Exception {
        UUID owner = UUID.randomUUID();
        LearnerOnboardingService service = mock(LearnerOnboardingService.class);
        when(service.complete(eq(owner), eq(OnboardingState.SKIPPED), eq(0L))).thenReturn(skipped(owner));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new OnboardingController(service)).build();

        mvc.perform(post("/api/me/onboarding/complete").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"SKIPPED\",\"version\":0}")
                        .requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                                new AuthPrincipal(owner, "owner@example.com", "Owner", com.ieltsaitutor.auth.UserRole.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value(OnboardingState.SKIPPED.name()));
        mvc.perform(post("/api/me/onboarding/complete").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"SKIPPED\",\"version\":0}"))
                .andExpect(status().isUnauthorized());

        verify(service).complete(owner, OnboardingState.SKIPPED, 0L);
    }

    private LearnerOnboardingProfile goal(UUID owner) {
        return new LearnerOnboardingProfile(owner, "BEGINNER", 5.0, null, Skill.READING, 30, 3,
                OnboardingState.COMPLETED, "SELF_REPORTED", "LEARNER_DECLARATION", null, null, null,
                1L, Instant.now(), Instant.now());
    }

    private LearnerOnboardingProfile skipped(UUID owner) {
        return new LearnerOnboardingProfile(owner, null, null, null, null, null, null, OnboardingState.SKIPPED,
                "SELF_REPORTED", "LEARNER_DECLARATION", null, null, null, 1L, Instant.now(), Instant.now());
    }
}