package com.ieltsaitutor.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final Duration SESSION_LIFETIME = Duration.ofDays(30);
    private final AuthUserRepository users;
    private final AuthSessionRepository sessions;
    private final PasswordHasher passwords;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AuthService(AuthUserRepository users, AuthSessionRepository sessions, PasswordHasher passwords) {
        this(users, sessions, passwords, Clock.systemUTC());
    }

    AuthService(AuthUserRepository users, AuthSessionRepository sessions, PasswordHasher passwords, Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
        this.clock = clock;
    }

    public AuthSessionResponse register(RegisterCommand command) {
        String email = normalizeEmail(command.email());
        String firstName = command.firstName().trim();
        if (users.findByEmail(email) != null) {
            throw new AuthException("AUTH_EMAIL_EXISTS", HttpStatus.CONFLICT, "Email này đã được đăng ký.");
        }
        AuthUser user = users.save(new AuthUser(UUID.randomUUID(), email, firstName,
                passwords.hash(command.password()), UserRole.CUSTOMER, clock.instant()));
        return createSession(user);
    }

    public AuthSessionResponse login(LoginCommand command) {
        AuthUser user = users.findByEmail(normalizeEmail(command.email()));
        if (user == null || !passwords.matches(command.password(), user.passwordHash())) {
            throw new AuthException("AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED,
                    "Email hoặc mật khẩu chưa đúng.");
        }
        return createSession(user);
    }

    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        sessions.revoke(hashToken(stripBearer(rawToken)), clock.instant());
    }

    public AuthSessionResponse me(String rawToken) {
        AuthUser user = authenticate(rawToken);
        return new AuthSessionResponse(null, AuthUserView.from(user));
    }

    public AuthUser authenticate(String rawToken) {
        String token = stripBearer(rawToken);
        if (token.isBlank()) throw unauthorized();
        AuthSession session = sessions.findActiveByTokenHash(hashToken(token), clock.instant());
        if (session == null) throw unauthorized();
        AuthUser user = users.findById(session.userId());
        if (user == null) throw unauthorized();
        return user;
    }

    private AuthSessionResponse createSession(AuthUser user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        sessions.save(new AuthSession(UUID.randomUUID(), user.id(), hashToken(token),
                now.plus(SESSION_LIFETIME), now, null));
        return new AuthSessionResponse(token, AuthUserView.from(user));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) throw new AuthException("AUTH_INVALID_REQUEST", HttpStatus.BAD_REQUEST,
                "Email là bắt buộc.");
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String stripBearer(String rawToken) {
        if (rawToken == null) return "";
        String token = rawToken.trim();
        return token.regionMatches(true, 0, "Bearer ", 0, 7) ? token.substring(7).trim() : token;
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Token hashing is unavailable", exception);
        }
    }

    private AuthException unauthorized() {
        return new AuthException("AUTH_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }
}
