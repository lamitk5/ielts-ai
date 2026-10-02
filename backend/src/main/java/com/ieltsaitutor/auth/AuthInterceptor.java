package com.ieltsaitutor.auth;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String PRINCIPAL_ATTRIBUTE = "com.ieltsaitutor.auth.AuthInterceptor.principal";
    private final AuthService service;

    public AuthInterceptor(AuthService service) { this.service = service; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && !authorization.isBlank()) {
            try {
                AuthUser user = service.authenticate(authorization);
                request.setAttribute(PRINCIPAL_ATTRIBUTE, new AuthPrincipal(user.id(), user.email(), user.firstName(), user.role()));
            } catch (AuthException ignored) {
                if (requiresAuthentication(request)) return reject(response);
            }
        } else if (requiresAuthentication(request)) {
            return reject(response);
        }
        if (requiresAdmin(request)) {
            Object principal = request.getAttribute(PRINCIPAL_ATTRIBUTE);
            if (!(principal instanceof AuthPrincipal authenticated && authenticated.role() == UserRole.ADMIN)) {
                return rejectForbidden(response);
            }
        }
        return true;
    }

    private boolean requiresAuthentication(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/auth/me") || path.startsWith("/api/me/")
                || path.startsWith("/api/submissions")
                || path.startsWith("/api/results")
                || path.startsWith("/api/admin/submissions")
                || path.equals("/api/user/preferences")
                || path.startsWith("/api/ai/attachments")
                || path.startsWith("/api/learning/drafts")
                || path.matches("/api/practice/[^/]+/attempts") || path.startsWith("/api/practice/writing/submissions")
                || path.startsWith("/api/practice/speaking/attempts")
                || path.startsWith("/api/admin/practice-generator");
    }

    private boolean requiresAdmin(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/admin/practice-generator") || path.startsWith("/api/admin/submissions");
    }

    private boolean reject(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":{\"code\":\"AUTH_UNAUTHORIZED\",\"message\":\"Đăng nhập để tiếp tục.\"}}");
        return false;
    }

    private boolean rejectForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":{\"code\":\"AUTH_FORBIDDEN\",\"message\":\"Quyền quản trị viên là bắt buộc.\"}}");
        return false;
    }
}
