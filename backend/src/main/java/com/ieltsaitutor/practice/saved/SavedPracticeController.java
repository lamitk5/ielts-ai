package com.ieltsaitutor.practice.saved;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/me/saved-practices")
public class SavedPracticeController {
    private final SavedPracticeService service;

    public SavedPracticeController(SavedPracticeService service) {
        this.service = service;
    }

    @PostMapping("/{publishedSetId}")
    public SavedPractice save(@PathVariable String publishedSetId, HttpServletRequest request) {
        UUID userId = principal(request).userId();
        try {
            return service.save(userId, publishedSetId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @DeleteMapping("/{publishedSetId}")
    public ResponseEntity<Void> unsave(@PathVariable String publishedSetId, HttpServletRequest request) {
        UUID userId = principal(request).userId();
        service.unsave(userId, publishedSetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<SavedPractice> list(
            @RequestParam(required = false) String skill,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        UUID userId = principal(request).userId();
        return service.list(userId, skill, page, size);
    }

    @GetMapping("/{publishedSetId}/status")
    public Map<String, Boolean> status(@PathVariable String publishedSetId, HttpServletRequest request) {
        UUID userId = principal(request).userId();
        boolean isSaved = service.isSaved(userId, publishedSetId);
        return Map.of("saved", isSaved);
    }

    private AuthPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (value instanceof AuthPrincipal principal) {
            return principal;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }
}
