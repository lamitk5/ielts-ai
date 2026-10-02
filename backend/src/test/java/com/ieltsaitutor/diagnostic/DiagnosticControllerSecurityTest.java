package com.ieltsaitutor.diagnostic;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;

class DiagnosticControllerSecurityTest {
    @Test void unauthenticatedCannotReadDiagnostic() throws Exception {
        MockMvc mvc=MockMvcBuilders.standaloneSetup(new DiagnosticController(mock(DiagnosticSessionService.class),mock(DiagnosticSubmissionService.class),mock(DiagnosticAggregationService.class))).build();
        mvc.perform(get("/api/me/diagnostic/session")).andExpect(status().isUnauthorized());
    }
}
