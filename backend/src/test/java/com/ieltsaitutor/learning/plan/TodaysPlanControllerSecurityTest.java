package com.ieltsaitutor.learning.plan;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TodaysPlanControllerSecurityTest {
    @Test void unauthenticatedLearnerCannotReadPlan() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new TodaysPlanController(mock(TodaysPlanService.class))).build();
        mvc.perform(get("/api/me/today")).andExpect(status().isUnauthorized());
    }
}
