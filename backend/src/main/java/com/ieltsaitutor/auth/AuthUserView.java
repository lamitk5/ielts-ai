package com.ieltsaitutor.auth;

import java.util.UUID;

public record AuthUserView(UUID id, String email, String firstName, UserRole role) {
    public static AuthUserView from(AuthUser user) {
        return new AuthUserView(user.id(), user.email(), user.firstName(), user.role());
    }
}
