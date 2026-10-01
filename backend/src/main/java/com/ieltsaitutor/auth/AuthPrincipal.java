package com.ieltsaitutor.auth;

import java.util.UUID;

public record AuthPrincipal(UUID userId, String email, String firstName, UserRole role) {}
