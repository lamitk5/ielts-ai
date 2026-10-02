package com.ieltsaitutor.speaking;

import java.io.InputStream;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;

@RestController
@RequestMapping("/api/practice/speaking/submissions")
public class SpeakingAudioController {

    private final SpeakingAudioStorage audioStorage;
    private final SpeakingAudioValidator audioValidator;
    private final PracticeSubmissionRepository submissionRepository;
    private final SpeakingSubmissionRepository speakingSubmissionRepository;

    public SpeakingAudioController(
            SpeakingAudioStorage audioStorage,
            SpeakingAudioValidator audioValidator,
            PracticeSubmissionRepository submissionRepository,
            SpeakingSubmissionRepository speakingSubmissionRepository) {
        this.audioStorage = audioStorage;
        this.audioValidator = audioValidator;
        this.submissionRepository = submissionRepository;
        this.speakingSubmissionRepository = speakingSubmissionRepository;
    }

    @PostMapping("/{submissionId}/audio")
    public SpeakingAudioUploadResponse uploadAudio(
            @PathVariable UUID submissionId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        AuthPrincipal principal = principal(request);
        PracticeSubmission submission = submissionRepository.findByOwnerAndId(principal.userId(), submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found or access denied"));

        if (!submission.editable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Submission is no longer editable");
        }

        try {
            audioValidator.validate(file.getContentType(), file.getOriginalFilename(), file.getSize(), file.getBytes());

            SpeakingAudioReference ref = audioStorage.store(
                    principal.userId(),
                    submissionId,
                    file.getContentType(),
                    file.getInputStream(),
                    file.getSize());

            SpeakingSubmission speaking = speakingSubmissionRepository.findBySubmissionId(submissionId)
                    .orElseGet(() -> new SpeakingSubmission(
                            UUID.randomUUID(),
                            submissionId,
                            submission.practiceId(),
                            submission.practiceVersionId(),
                            0,
                            0,
                            null,
                            null,
                            null,
                            null,
                            "UNAVAILABLE",
                            SpeakingSubmissionState.IN_PROGRESS,
                            java.time.Instant.now(),
                            java.time.Instant.now()));

            SpeakingSubmission updated = speaking.withAudio(ref.storageKey(), ref.mimeType(), ref.sizeBytes());
            speakingSubmissionRepository.save(updated);

            return new SpeakingAudioUploadResponse(
                    submissionId,
                    ref.storageKey(),
                    ref.mimeType(),
                    ref.sizeBytes());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store audio");
        }
    }

    @GetMapping("/{submissionId}/audio")
    public ResponseEntity<Resource> streamAudio(
            @PathVariable UUID submissionId,
            HttpServletRequest request) {

        AuthPrincipal principal = principal(request);
        boolean isAdmin = principal.role() == UserRole.ADMIN;

        PracticeSubmission submission;
        if (isAdmin) {
            submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));
        } else {
            submission = submissionRepository.findByOwnerAndId(principal.userId(), submissionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found or access denied"));
        }

        SpeakingSubmission speaking = speakingSubmissionRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Speaking submission audio record not found"));

        if (speaking.audioStorageKey() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No audio recorded for this submission");
        }

        try {
            InputStream stream = audioStorage.read(speaking.audioStorageKey());
            MediaType mediaType = speaking.audioMimeType() != null
                    ? MediaType.parseMediaType(speaking.audioMimeType())
                    : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speaking-audio.bin\"")
                    .body(new InputStreamResource(stream));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Audio file not available");
        }
    }

    private AuthPrincipal principal(HttpServletRequest request) {
        Object principal = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (principal instanceof AuthPrincipal authenticated) return authenticated;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }

    public record SpeakingAudioUploadResponse(
            UUID submissionId,
            String storageKey,
            String audioMimeType,
            long audioSizeBytes) {}
}
