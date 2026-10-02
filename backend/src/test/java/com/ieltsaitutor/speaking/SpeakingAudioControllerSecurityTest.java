package com.ieltsaitutor.speaking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import com.ieltsaitutor.submission.SubmissionStatus;

class SpeakingAudioControllerSecurityTest {

    private SpeakingAudioStorage audioStorage;
    private SpeakingAudioValidator audioValidator;
    private PracticeSubmissionRepository submissionRepository;
    private SpeakingSubmissionRepository speakingSubmissionRepository;
    private SpeakingAudioController controller;

    private UUID userA;
    private UUID userB;
    private UUID submissionId;

    @BeforeEach
    void setUp() {
        audioStorage = mock(SpeakingAudioStorage.class);
        audioValidator = new SpeakingAudioValidator();
        submissionRepository = mock(PracticeSubmissionRepository.class);
        speakingSubmissionRepository = mock(SpeakingSubmissionRepository.class);

        controller = new SpeakingAudioController(
                audioStorage, audioValidator, submissionRepository, speakingSubmissionRepository);

        userA = UUID.randomUUID();
        userB = UUID.randomUUID();
        submissionId = UUID.randomUUID();
    }

    @Test
    @DisplayName("User A successfully uploads audio to own submission")
    void userAUploadsAudioToOwnSubmission() throws Exception {
        PracticeSubmission sub = new PracticeSubmission(
                submissionId, userA, "SPEAKING", "speaking-part-1-01", "v1", "set-1", 1,
                SubmissionStatus.IN_PROGRESS, Instant.now().minusSeconds(30), Instant.now(), null, null,
                0, "k1", null, null, false, Instant.now().minusSeconds(30), Instant.now());

        when(submissionRepository.findByOwnerAndId(userA, submissionId)).thenReturn(Optional.of(sub));
        when(speakingSubmissionRepository.findBySubmissionId(submissionId)).thenReturn(Optional.of(
                new SpeakingSubmission(UUID.randomUUID(), submissionId, "speaking-part-1-01", "v1", 0, 30, null, null, null, null, "UNAVAILABLE", SpeakingSubmissionState.IN_PROGRESS, Instant.now(), Instant.now())
        ));

        SpeakingAudioReference mockRef = new SpeakingAudioReference(
                "speaking/key123.bin", "audio/webm", 1024L, "sha256");
        when(audioStorage.store(eq(userA), eq(submissionId), eq("audio/webm"), any(), eq(1024L)))
                .thenReturn(mockRef);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE, new AuthPrincipal(userA, "userA@example.com", "UserA", UserRole.CUSTOMER));

        MockMultipartFile file = new MockMultipartFile(
                "file", "audio.webm", "audio/webm", new byte[1024]);

        var response = controller.uploadAudio(submissionId, file, request);
        assertNotNull(response);
        assertEquals("audio/webm", response.audioMimeType());
        assertEquals(1024L, response.audioSizeBytes());
        // Verify key is not a full local path
        assertFalse(response.storageKey().contains("C:\\"));
    }

    @Test
    @DisplayName("User B cannot upload audio to User A's submission (403/404 Forbidden)")
    void userBCannotUploadToUserASubmission() {
        when(submissionRepository.findByOwnerAndId(userB, submissionId)).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE, new AuthPrincipal(userB, "userB@example.com", "UserB", UserRole.CUSTOMER));

        MockMultipartFile file = new MockMultipartFile(
                "file", "audio.webm", "audio/webm", new byte[1024]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.uploadAudio(submissionId, file, request));
        assertTrue(ex.getStatusCode() == HttpStatus.NOT_FOUND || ex.getStatusCode() == HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("User B cannot stream audio belonging to User A")
    void userBCannotStreamUserAAudio() {
        when(submissionRepository.findByOwnerAndId(userB, submissionId)).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE, new AuthPrincipal(userB, "userB@example.com", "UserB", UserRole.CUSTOMER));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.streamAudio(submissionId, request));
        assertTrue(ex.getStatusCode() == HttpStatus.NOT_FOUND || ex.getStatusCode() == HttpStatus.FORBIDDEN);
    }
}
