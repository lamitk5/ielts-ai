package com.ieltsaitutor.rag.admin;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;

public class AdminTokenInterceptor implements HandlerInterceptor {
    private final AdminAuthorizationService authorization;

    public AdminTokenInterceptor(AdminAuthorizationService authorization) { this.authorization = authorization; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!request.getRequestURI().startsWith("/api/admin/rag/")) return true;
        Object principal = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (principal instanceof AuthPrincipal authenticated && authenticated.role() == UserRole.ADMIN) return true;
        AuthorizationDecision decision = authorization.authorize(request.getHeader("X-Admin-Token"));
        if (decision.authorized()) return true;
        response.setStatus(decision.statusCode());
        response.setContentType("application/json");
        response.getWriter().write(decision.statusCode() == 401
                ? "{\"error\":{\"code\":\"RAG_ADMIN_UNAUTHORIZED\",\"message\":\"Admin authorization is required.\"}}"
                : "{\"error\":{\"code\":\"RAG_ADMIN_FORBIDDEN\",\"message\":\"Admin authorization is invalid.\"}}");
        return false;
    }
}
