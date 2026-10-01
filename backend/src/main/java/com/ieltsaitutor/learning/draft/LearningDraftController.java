package com.ieltsaitutor.learning.draft;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/learning/drafts")
public class LearningDraftController {
    private final LearningDraftService service;

    public LearningDraftController(LearningDraftService service) {
        this.service = service;
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentDraft(
            @RequestParam String skill,
            @RequestParam String referenceId,
            @RequestAttribute(name = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", Map.of("code", "AUTH_UNAUTHORIZED", "message", "Đăng nhập để tiếp tục.")
            ));
        }

        Optional<LearningDraft> draftOpt = service.getCurrentDraft(principal.userId(), skill, referenceId);
        if (draftOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", Map.of("code", "DRAFT_NOT_FOUND", "message", "Không tìm thấy bản nháp.")
            ));
        }

        return ResponseEntity.ok(draftOpt.get());
    }

    @PutMapping
    public ResponseEntity<?> saveDraft(
            @RequestBody SaveDraftRequest request,
            @RequestAttribute(name = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", Map.of("code", "AUTH_UNAUTHORIZED", "message", "Đăng nhập để tiếp tục.")
            ));
        }

        LearningDraft draft = service.saveDraft(
                principal.userId(),
                request.skill(),
                request.referenceId(),
                request.contentSnapshot(),
                request.expectedVersion()
        );
        return ResponseEntity.ok(draft);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDraft(
            @PathVariable UUID id,
            @RequestAttribute(name = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", Map.of("code", "AUTH_UNAUTHORIZED", "message", "Đăng nhập để tiếp tục.")
            ));
        }

        service.deleteDraft(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(DraftConflictException.class)
    public ResponseEntity<?> handleConflict(DraftConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", Map.of("code", ex.code(), "message", ex.getMessage()),
                "timestamp", Instant.now()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", Map.of("code", "INVALID_ARGUMENT", "message", ex.getMessage()),
                "timestamp", Instant.now()
        ));
    }

    public record SaveDraftRequest(
            String skill,
            String referenceId,
            String contentSnapshot,
            Long expectedVersion
    ) {}
}
