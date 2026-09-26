package com.ieltsaitutor.preferences;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ieltsaitutor.auth.AuthException;
import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/user/preferences")
public class UserPreferencesController {
    private final UserPreferencesService service;

    public UserPreferencesController(UserPreferencesService service) { this.service = service; }

    @GetMapping
    public UserPreferences get(HttpServletRequest request) { return service.get(principal(request).userId()); }

    @PutMapping
    public UserPreferences update(HttpServletRequest request, @RequestBody UserPreferences preferences) {
        AuthPrincipal principal = principal(request);
        if (preferences.version() == null) {
            throw new AuthException("PREFERENCES_INVALID_REQUEST", HttpStatus.BAD_REQUEST, "Thiết lập chưa hợp lệ.");
        }
        return service.update(principal.userId(), preferences, preferences.version());
    }

    private AuthPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (value instanceof AuthPrincipal principal) return principal;
        throw new AuthException("AUTH_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }
}
