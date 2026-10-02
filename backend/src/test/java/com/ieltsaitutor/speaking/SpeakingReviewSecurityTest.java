package com.ieltsaitutor.speaking;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpeakingReviewSecurityTest {

    private SpeakingReviewService reviewService;
    private MockMvc mockMvc;

    private final UUID submissionId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID learnerId = UUID.randomUUID();
    private final AuthPrincipal admin = new AuthPrincipal(adminId, "admin@test.com", "Admin", UserRole.ADMIN);
    private final AuthPrincipal learner = new AuthPrincipal(learnerId, "learner@test.com", "Learner", UserRole.CUSTOMER);

    @BeforeEach
    void setUp() {
        reviewService = mock(SpeakingReviewService.class);
        SpeakingReviewController controller = new SpeakingReviewController(reviewService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Unauthenticated request to review is rejected with 401")
    void unauthenticatedRequestRejected() throws Exception {
        mockMvc.perform(get("/api/practice/speaking/submissions/" + submissionId + "/review"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Learner attempting to submit review gets 403 Forbidden")
    void learnerCannotSubmitReviewHttp() throws Exception {
        when(reviewService.submitReview(any(), eq(submissionId), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new SecurityException("Chỉ quản trị viên/giám khảo mới có quyền chấm bài Speaking"));

        mockMvc.perform(post("/api/practice/speaking/submissions/" + submissionId + "/review")
                        .requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE, learner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "overallBand": 7.0,
                                    "fluencyCoherence": 7.0,
                                    "lexicalResource": 7.0,
                                    "grammaticalRange": 7.0,
                                    "pronunciation": 7.0,
                                    "reviewerFeedback": "Good"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin submits review successfully returning 200 with review details")
    void adminSubmitsReviewSuccessfully() throws Exception {
        SpeakingReview review = new SpeakingReview(
                UUID.randomUUID(), submissionId, adminId, 1,
                8.0, 8.0, 8.0, 8.0, 8.0,
                "Excellent fluency and accurate pronunciation.",
                null, "COMPLETED", Instant.now(), Instant.now()
        );

        when(reviewService.submitReview(
                eq(admin), eq(submissionId), eq(8.0), eq(8.0), eq(8.0), eq(8.0), eq(8.0),
                eq("Excellent fluency and accurate pronunciation."), any()
        )).thenReturn(review);

        mockMvc.perform(post("/api/practice/speaking/submissions/" + submissionId + "/review")
                        .requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "overallBand": 8.0,
                                    "fluencyCoherence": 8.0,
                                    "lexicalResource": 8.0,
                                    "grammaticalRange": 8.0,
                                    "pronunciation": 8.0,
                                    "reviewerFeedback": "Excellent fluency and accurate pronunciation."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallBand").value(8.0))
                .andExpect(jsonPath("$.reviewVersion").value(1));
    }
}
