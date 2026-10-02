package com.ieltsaitutor.learning.notebook;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ErrorNotebookControllerSecurityTest {
    @Test void unauthenticatedLearnerCannotReadNotebook() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ErrorNotebookController(mock(ErrorNotebookService.class), mock(ErrorNotebookActionService.class))).build();
        mvc.perform(get("/api/me/error-notebook")).andExpect(status().isUnauthorized());
    }
}
