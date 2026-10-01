package com.ieltsaitutor.tutor.memory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;

class ConversationControllerTest {
    private final ConversationService service = mock(ConversationService.class);
    private MockMvc mvc;
    private UUID userId;
    private UUID conversationId;

    @BeforeEach void setUp() { mvc = MockMvcBuilders.standaloneSetup(new ConversationController(service)).build(); userId = UUID.randomUUID(); conversationId = UUID.randomUUID(); }

    @Test void foreignConversationIsNotExposed() throws Exception {
        when(service.findOwned(userId, conversationId)).thenReturn(Optional.empty());
        mvc.perform(get("/api/ai/conversations/{id}", conversationId).requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                new AuthPrincipal(userId, "student@example.com", "Mai", UserRole.CUSTOMER))).andExpect(status().isNotFound());
    }

    @Test void archiveRequiresAuthenticatedOwner() throws Exception {
        when(service.archive(userId, conversationId)).thenReturn(true);
        mvc.perform(post("/api/ai/conversations/{id}/archive", conversationId).requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                new AuthPrincipal(userId, "student@example.com", "Mai", UserRole.CUSTOMER))).andExpect(status().isOk());
    }
}
