package com.ieltsaitutor.auth;

import java.time.Instant;

public interface AuthSessionRepository {
    AuthSession save(AuthSession session);
    AuthSession findActiveByTokenHash(String tokenHash, Instant now);
    void revoke(String tokenHash, Instant revokedAt);
}
