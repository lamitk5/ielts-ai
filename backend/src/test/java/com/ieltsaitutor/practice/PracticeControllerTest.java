package com.ieltsaitutor.practice;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;

class PracticeControllerTest {
    private final PracticeService service = mock(PracticeService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() { mvc = MockMvcBuilders.standaloneSetup(new PracticeController(service)).build(); }

    @Test
    void publicSetViewDoesNotExposeStoredAnswerKey() throws Exception {
        PracticeSet set = new PracticeSet("reading-foundation-01", "Reading", "Reading foundation", "Synthetic", List.of(
                new PracticeQuestion("q1", "Question?", List.of("A", "B"), "B", "Because.")));
        when(service.set("reading", "reading-foundation-01")).thenReturn(set);

        mvc.perform(get("/api/practice/reading/sets/reading-foundation-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].id").value("q1"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("answerKey"))));
    }

    @Test
    void submittedAttemptReturnsReferenceWithoutAnswerKey() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID attemptId = UUID.randomUUID();
        when(service.submit("reading", "reading-foundation-01", Map.of("q1", "B"), userId))
                .thenReturn(new PracticeAttemptResult(attemptId, 1, 1, Map.of("q1", "B"),
                        List.of(new PracticeReview("q1", "B", "B", true, "Because."))));

        mvc.perform(post("/api/practice/reading/attempts")
                        .requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                                new AuthPrincipal(userId, "user@test", "User", UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setId\":\"reading-foundation-01\",\"answers\":{\"q1\":\"B\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(attemptId.toString()))
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("correctAnswer"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("answer_payload"))));
    }
}
