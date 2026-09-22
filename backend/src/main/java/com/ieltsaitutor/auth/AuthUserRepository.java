package com.ieltsaitutor.auth;

import java.util.UUID;

public interface AuthUserRepository {
    AuthUser findByEmail(String email);
    AuthUser findById(UUID id);
    AuthUser save(AuthUser user);
}
