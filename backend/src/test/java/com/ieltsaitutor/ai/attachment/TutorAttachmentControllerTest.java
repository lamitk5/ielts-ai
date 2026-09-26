package com.ieltsaitutor.ai.attachment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ieltsaitutor.ai.attachment.TutorAttachment.AttachmentStatus;
import com.ieltsaitutor.auth.AuthException;
import com.ieltsaitutor.auth.AuthExceptionHandler;
import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthService;
import com.ieltsaitutor.auth.AuthUser;
import com.ieltsaitutor.auth.UserRole;

class TutorAttachmentControllerTest {
    private static final String BASE_PATH = "/api/ai/attachments";
    private final AuthService auth = mock(AuthService.class);
    private final TutorAttachmentService service = mock(TutorAttachmentService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TutorAttachmentController(service))
                .addInterceptors(new AuthInterceptor(auth))
                .setControllerAdvice(new AuthExceptionHandler())
                .build();
    }

    @Test
    void unauthenticatedRequestsReturnNormalized401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "essay.pdf", "application/pdf", "dummy content".getBytes());
        mvc.perform(multipart(BASE_PATH).file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_UNAUTHORIZED"));

        mvc.perform(get(BASE_PATH + "/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_UNAUTHORIZED"));

        mvc.perform(delete(BASE_PATH + "/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void authenticatedUploadAcceptsPdfDocxTxtUnder10MiB() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);

        UUID attachmentId = UUID.randomUUID();
        TutorAttachment attachment = new TutorAttachment(
                attachmentId,
                userId,
                "essay.pdf",
                "application/pdf",
                1024L,
                AttachmentStatus.READY,
                null,
                Instant.parse("2026-09-26T10:00:00Z"),
                Instant.parse("2026-09-26T10:00:00Z")
        );

        when(service.upload(eq(userId), any())).thenReturn(attachment);

        MockMultipartFile file = new MockMultipartFile("file", "essay.pdf", "application/pdf", new byte[1024]);
        mvc.perform(multipart(BASE_PATH)
                        .file(file)
                        .header("Authorization", "Bearer valid"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(attachmentId.toString()))
                .andExpect(jsonPath("$.filename").value("essay.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.sizeBytes").value(1024))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void authenticatedUploadRejectsFileExceeding10MiB() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);

        when(service.upload(eq(userId), any()))
                .thenThrow(new AuthException("ATTACHMENT_SIZE_EXCEEDED", HttpStatus.BAD_REQUEST, "Dung lượng tệp vượt quá 10MB."));

        MockMultipartFile oversizedFile = new MockMultipartFile("file", "huge.pdf", "application/pdf", new byte[100]);
        mvc.perform(multipart(BASE_PATH)
                        .file(oversizedFile)
                        .header("Authorization", "Bearer valid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ATTACHMENT_SIZE_EXCEEDED"));
    }

    @Test
    void authenticatedUploadRejectsHtmlOrExecutableTypes() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);

        when(service.upload(eq(userId), any()))
                .thenThrow(new AuthException("ATTACHMENT_TYPE_NOT_SUPPORTED", HttpStatus.BAD_REQUEST, "Định dạng tệp không được hỗ trợ. Chỉ chấp nhận PDF, DOCX hoặc TXT."));

        MockMultipartFile scriptFile = new MockMultipartFile("file", "script.sh", "application/x-sh", "echo bad".getBytes());
        mvc.perform(multipart(BASE_PATH)
                        .file(scriptFile)
                        .header("Authorization", "Bearer valid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ATTACHMENT_TYPE_NOT_SUPPORTED"));
    }

    @Test
    void authenticatedUploadEnforcesOneActiveAttachmentLimit() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);

        when(service.upload(eq(userId), any()))
                .thenThrow(new AuthException("ATTACHMENT_LIMIT_EXCEEDED", HttpStatus.CONFLICT, "Mỗi yêu cầu chỉ hỗ trợ tối đa 1 tệp đính kèm đang hoạt động."));

        MockMultipartFile secondFile = new MockMultipartFile("file", "draft2.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[512]);
        mvc.perform(multipart(BASE_PATH)
                        .file(secondFile)
                        .header("Authorization", "Bearer valid"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ATTACHMENT_LIMIT_EXCEEDED"));
    }

    @Test
    void authenticatedGetEnforcesOwnershipAndReturnsAttachment() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);

        UUID attachmentId = UUID.randomUUID();
        TutorAttachment attachment = new TutorAttachment(
                attachmentId,
                userId,
                "notes.txt",
                "text/plain",
                256L,
                AttachmentStatus.READY,
                null,
                Instant.parse("2026-09-26T10:00:00Z"),
                Instant.parse("2026-09-26T10:00:00Z")
        );

        when(service.get(userId, attachmentId)).thenReturn(attachment);

        mvc.perform(get(BASE_PATH + "/" + attachmentId)
                        .header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(attachmentId.toString()))
                .andExpect(jsonPath("$.filename").value("notes.txt"))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void authenticatedGetReturnsNotFoundForNonOwnedOrMissingAttachment() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);

        UUID attachmentId = UUID.randomUUID();
        when(service.get(userId, attachmentId))
                .thenThrow(new AuthException("ATTACHMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy tệp đính kèm."));

        mvc.perform(get(BASE_PATH + "/" + attachmentId)
                        .header("Authorization", "Bearer valid"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ATTACHMENT_NOT_FOUND"));
    }

    @Test
    void authenticatedDeleteEnforcesOwnershipAndReturnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);

        UUID attachmentId = UUID.randomUUID();

        mvc.perform(delete(BASE_PATH + "/" + attachmentId)
                        .header("Authorization", "Bearer valid"))
                .andExpect(status().isNoContent());
    }

    @Test
    void authenticatedDeleteReturnsNotFoundForForeignAttachment() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);

        UUID foreignId = UUID.randomUUID();
        doThrow(new AuthException("ATTACHMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy tệp đính kèm."))
                .when(service).delete(userId, foreignId);

        mvc.perform(delete(BASE_PATH + "/" + foreignId)
                        .header("Authorization", "Bearer valid"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ATTACHMENT_NOT_FOUND"));
    }

    private void authenticate(UUID userId) {
        when(auth.authenticate("Bearer valid")).thenReturn(new AuthUser(userId, "learner@example.com", "Linh", "hash",
                UserRole.CUSTOMER, Instant.parse("2026-09-26T00:00:00Z")));
    }
}
