package com.ieltsaitutor.rag.admin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminTokenAuthorizationService implements AdminAuthorizationService {
    @Value("${rag.admin-token:}")
    private String configuredToken = "";

    public AdminTokenAuthorizationService() {}
    public AdminTokenAuthorizationService(String configuredToken) {
        this.configuredToken = configuredToken == null ? "" : configuredToken;
    }

    @Override
    public AuthorizationDecision authorize(String providedToken) {
        if (configuredToken.isBlank() || providedToken == null || providedToken.isBlank()) {
            return new AuthorizationDecision(false, 401);
        }
        boolean matches = MessageDigest.isEqual(configuredToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8));
        return new AuthorizationDecision(matches, matches ? 200 : 403);
    }
}
