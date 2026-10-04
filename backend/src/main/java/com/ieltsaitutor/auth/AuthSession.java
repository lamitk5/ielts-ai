package com.ieltsaitutor.auth;

import java.time.Instant;
import java.util.UUID;

public record AuthSession(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant createdAt, Instant revokedAt) {}
