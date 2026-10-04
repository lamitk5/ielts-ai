package com.ieltsaitutor.auth;

import java.time.Instant;
import java.util.UUID;

public record AuthUser(UUID id, String email, String firstName, String passwordHash, UserRole role, Instant createdAt) {}
