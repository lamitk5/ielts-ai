package com.ieltsaitutor.profile;

import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthException;
import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/me")
public class LearnerProfileController {
    private final LearnerProfileService service;

    public LearnerProfileController(LearnerProfileService service) {
        this.service = service;
    }

    @GetMapping("/profile")
    public LearnerProfile getProfile(HttpServletRequest request) {
        UUID userId = principal(request).userId();
        return service.getProfile(userId);
    }

    @PutMapping("/profile")
    public LearnerProfile updateProfile(HttpServletRequest request, @RequestBody UpdateProfileCommand command) {
        UUID userId = principal(request).userId();
        return service.updateProfile(userId, command);
    }

    @PostMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(HttpServletRequest request, @RequestBody ChangePasswordCommand command) {
        UUID userId = principal(request).userId();
        service.changePassword(userId, command);
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công."));
    }

    private AuthPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (value instanceof AuthPrincipal principal) {
            return principal;
        }
        throw new AuthException("AUTH_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }
}
