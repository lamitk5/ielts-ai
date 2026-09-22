package com.ieltsaitutor.ai.controller;

import com.ieltsaitutor.ai.dto.AiChatResponse;
import com.ieltsaitutor.ai.dto.AiGrounding;
import com.ieltsaitutor.ai.dto.AiSource;
import com.ieltsaitutor.ai.service.AiChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiChatControllerTest {
    private AiChatService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(AiChatService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AiChatController(service))
                .setControllerAdvice(new com.ieltsaitutor.ai.exception.AiExceptionHandler())
                .build();
    }

    @Test
    void validChatReturnsApplicationResponse() throws Exception {
        when(service.chat(any())).thenReturn(answered("Giải thích rõ ràng."));

        mvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Giải thích present perfect","context":{"skill":"WRITING"},"history":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANSWERED"))
                .andExpect(jsonPath("$.answer").value("Giải thích rõ ràng."))
                .andExpect(jsonPath("$.sources").isArray())
                .andExpect(jsonPath("$.sources").isEmpty())
                .andExpect(jsonPath("$.grounding.status").value("NOT_ENABLED"));
    }

    @Test
    void blankMessageReturnsInvalidRequest() throws Exception {
        mvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AI_INVALID_REQUEST"));
    }

    @Test
    void malformedSkillReturnsInvalidRequest() throws Exception {
        mvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\",\"context\":{\"skill\":\"GRAMMAR\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AI_INVALID_REQUEST"));
    }

    private static AiChatResponse answered(String answer) {
        return new AiChatResponse(
                "ANSWERED", answer, List.<AiSource>of(),
                new AiGrounding("NOT_ENABLED", false),
                new AiChatResponse.Meta(UUID.randomUUID().toString()), Instant.now());
    }
}
