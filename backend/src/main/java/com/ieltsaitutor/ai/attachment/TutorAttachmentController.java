package com.ieltsaitutor.ai.attachment;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ieltsaitutor.auth.AuthException;
import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/ai/attachments")
public class TutorAttachmentController {
    private final TutorAttachmentService service;

    public TutorAttachmentController(TutorAttachmentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TutorAttachment> upload(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "requestId", required = false) String requestId) {
        AuthPrincipal principal = principal(request);
        TutorAttachment attachment = requestId == null
                ? service.upload(principal.userId(), file)
                : service.upload(principal.userId(), file, requestId);
        return ResponseEntity.status(HttpStatus.CREATED).body(attachment);
    }

    @GetMapping("/{id}")
    public TutorAttachment get(
            HttpServletRequest request,
            @PathVariable("id") UUID id) {
        AuthPrincipal principal = principal(request);
        return service.get(principal.userId(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            HttpServletRequest request,
            @PathVariable("id") UUID id) {
        AuthPrincipal principal = principal(request);
        service.delete(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    private AuthPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (value instanceof AuthPrincipal principal) {
            return principal;
        }
        throw new AuthException("AUTH_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }
}
