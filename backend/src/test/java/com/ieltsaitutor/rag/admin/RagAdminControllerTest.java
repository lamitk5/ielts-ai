package com.ieltsaitutor.rag.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ieltsaitutor.rag.admin.dto.RagActionResponse;
import com.ieltsaitutor.rag.admin.dto.RagDocumentDetail;
import com.ieltsaitutor.rag.admin.dto.RagDocumentSummary;
import com.ieltsaitutor.rag.admin.dto.RagPreviewResponse;
import com.ieltsaitutor.rag.ingestion.RagInvalidStateException;

class RagAdminControllerTest {
    private RagAdminService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(RagAdminService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RagAdminController(service)).setControllerAdvice(new RagAdminExceptionHandler()).build();
    }

    @Test
    void uploadRequiresMultipartMetadata() throws Exception {
        mvc.perform(multipart("/api/admin/rag/documents/upload")
                .file(new MockMultipartFile("file", "guide.pdf", "application/pdf", "data".getBytes())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadAlwaysStartsPendingReview() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.upload(any(), any())).thenReturn(new RagActionResponse("UPLOADED", id, "PENDING_REVIEW", "NOT_INDEXED", false));

        mvc.perform(multipart("/api/admin/rag/documents/upload")
                .file(new MockMultipartFile("file", "guide.pdf", "application/pdf", "data".getBytes()))
                .file(new MockMultipartFile("metadata", "", MediaType.APPLICATION_JSON_VALUE,
                        "{\"title\":\"Guide\",\"language\":\"en\",\"skill\":\"GENERAL\",\"rightsStatus\":\"APPROVED\"}".getBytes())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rightsStatus").value("PENDING_REVIEW"));
    }

    @Test
    void listReturnsSafeSummary() throws Exception {
        when(service.list()).thenReturn(List.of(new RagDocumentSummary(UUID.randomUUID(), "Guide", "GENERAL", "PENDING_REVIEW", "NOT_INDEXED", false, Instant.now())));
        mvc.perform(get("/api/admin/rag/documents")).andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Guide"))
                .andExpect(jsonPath("$[0].embedding").doesNotExist());
    }

    @Test
    void detailReturnsBoundedPreview() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.detail(id)).thenReturn(new RagDocumentDetail(id, "Guide", "GENERAL", "PENDING_REVIEW", "NOT_INDEXED", false,
                new RagPreviewResponse("preview", 7), Instant.now()));
        mvc.perform(get("/api/admin/rag/documents/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.preview.text").value("preview"));
    }

    @Test
    void returnsStableErrorShape() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.detail(id)).thenThrow(new RagInvalidStateException("RAG_INVALID_STATE", "not available"));
        mvc.perform(get("/api/admin/rag/documents/{id}", id)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RAG_INVALID_STATE"));
    }
}
