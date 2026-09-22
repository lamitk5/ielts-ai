package com.ieltsaitutor.practice;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
}
