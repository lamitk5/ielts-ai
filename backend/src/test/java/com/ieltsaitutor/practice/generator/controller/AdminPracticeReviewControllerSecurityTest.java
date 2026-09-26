package com.ieltsaitutor.practice.generator.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ieltsaitutor.auth.AuthException;
import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthService;
import com.ieltsaitutor.auth.AuthUser;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.practice.generator.dto.GeneratedSetReviewPayload;
import com.ieltsaitutor.practice.generator.dto.ReviewActionResponse;
import com.ieltsaitutor.practice.generator.domain.GenerationState;
import com.ieltsaitutor.practice.generator.service.PracticeReviewService;
import org.springframework.http.HttpStatus;

class AdminPracticeReviewControllerSecurityTest {

    private PracticeReviewService reviewService;
    private AuthService authService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        reviewService = mock(PracticeReviewService.class);
        authService = mock(AuthService.class);
        AuthInterceptor authInterceptor = new AuthInterceptor(authService);
        AdminPracticeReviewController controller = new AdminPracticeReviewController(reviewService);

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(authInterceptor)
                .build();
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        UUID setId = UUID.randomUUID();
        mvc.perform(get("/api/admin/practice-generator/sets/{setId}", setId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        when(authService.authenticate("Bearer bad-token"))
                .thenThrow(new AuthException("AUTH_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Invalid token"));

        UUID setId = UUID.randomUUID();
        mvc.perform(get("/api/admin/practice-generator/sets/{setId}", setId)
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void customerRoleReturns403Forbidden() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(authService.authenticate("Bearer customer-token"))
                .thenReturn(new AuthUser(customerId, "student@test.com", "hash", "Student", UserRole.CUSTOMER, Instant.now()));

        UUID setId = UUID.randomUUID();
        mvc.perform(get("/api/admin/practice-generator/sets/{setId}", setId)
                        .header("Authorization", "Bearer customer-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void adminRoleAccessAllowed() throws Exception {
        UUID adminId = UUID.randomUUID();
        when(authService.authenticate("Bearer admin-token"))
                .thenReturn(new AuthUser(adminId, "admin@test.com", "hash", "Admin", UserRole.ADMIN, Instant.now()));

        UUID setId = UUID.randomUUID();
        when(reviewService.getReviewPayload(setId)).thenReturn(new GeneratedSetReviewPayload(
                null, null, null, null, List.of(), List.of(), List.of()
        ));

        mvc.perform(get("/api/admin/practice-generator/sets/{setId}", setId)
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanExecuteReviewAction() throws Exception {
        UUID adminId = UUID.randomUUID();
        when(authService.authenticate("Bearer admin-token"))
                .thenReturn(new AuthUser(adminId, "admin@test.com", "hash", "Admin", UserRole.ADMIN, Instant.now()));

        UUID setId = UUID.randomUUID();
        UUID actionId = UUID.randomUUID();
        ReviewActionResponse response = new ReviewActionResponse(
                actionId, setId, "APPROVE", GenerationState.APPROVED, adminId, Instant.now(), "Approved for publication"
        );
        when(reviewService.executeReview(eq(setId), any(), eq(adminId))).thenReturn(response);

        mvc.perform(post("/api/admin/practice-generator/sets/{setId}/review", setId)
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "action": "APPROVE",
                            "feedbackNotes": "High quality set"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("APPROVE"))
                .andExpect(jsonPath("$.resultingState").value("APPROVED"));
    }
}
