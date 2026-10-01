package com.ieltsaitutor.ai.attachment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthExceptionHandler;
import com.ieltsaitutor.auth.AuthService;
import com.ieltsaitutor.auth.AuthUser;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.tutor.memory.ConversationController;
import com.ieltsaitutor.tutor.memory.ConversationRepository;
import com.ieltsaitutor.tutor.memory.ConversationService;

class TutorAttachmentApiContractTest {
    private final AuthService auth = mock(AuthService.class);
    private final TutorAttachmentService attachmentService = mock(TutorAttachmentService.class);
    private final ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                new ConversationController(new ConversationService(conversationRepository)),
                new TutorAttachmentController(attachmentService))
                .addInterceptors(new AuthInterceptor(auth)).setControllerAdvice(new AuthExceptionHandler()).build();
    }

    @Test
    void createsOwnedConversationWithoutCallingAI() throws Exception {
        authenticate();
        when(conversationRepository.findConversation(any(), any())).thenReturn(java.util.Optional.empty());

        mvc.perform(post("/api/ai/conversations").header("Authorization", "Bearer valid"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void uploadsOneToFiveFiles() throws Exception {
        authenticate();
        UUID conversationId = UUID.randomUUID();

        mvc.perform(multipart("/api/ai/attachments")
                        .file(new MockMultipartFile("files", "one.txt", "text/plain", "one".getBytes()))
                        .file(new MockMultipartFile("files", "two.txt", "text/plain", "two".getBytes()))
                        .param("conversationId", conversationId.toString())
                        .header("Authorization", "Bearer valid"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attachments").isArray());
    }

    @Test
    void rejectsSixFiles() throws Exception {
        authenticate();
        var request = multipart("/api/ai/attachments").param("conversationId", UUID.randomUUID().toString())
                .header("Authorization", "Bearer valid");
        for (int i = 0; i < 6; i++) request.file(new MockMultipartFile("files", i + ".txt", "text/plain", "x".getBytes()));

        mvc.perform(request).andExpect(status().isBadRequest());
    }

    private void authenticate() {
        when(auth.authenticate("Bearer valid")).thenReturn(new AuthUser(UUID.randomUUID(), "qa@example.com", "QA", "hash",
                UserRole.CUSTOMER, java.time.Instant.now()));
    }
}
