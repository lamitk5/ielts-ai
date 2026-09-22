package com.ieltsaitutor.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class AuthServiceTest {
    private final PasswordHasher hasher = new PasswordHasher();
    private final FakeUserRepository users = new FakeUserRepository();
    private final FakeSessionRepository sessions = new FakeSessionRepository();
    private final AuthService service = new AuthService(users, sessions, hasher,
            Clock.fixed(Instant.parse("2026-09-22T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void springContextInstantiatesAuthServiceThroughDependencyInjection() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(AuthUserRepository.class, () -> mock(AuthUserRepository.class));
            context.registerBean(AuthSessionRepository.class, () -> mock(AuthSessionRepository.class));
            context.registerBean(PasswordHasher.class);
            context.registerBean(AuthService.class);

            context.refresh();

            assertThat(context.getBean(AuthService.class)).isNotNull();
        }
    }

    @Test
    void registersWithHashedPasswordAndReturnsAuthenticatedSession() {
        AuthSessionResponse result = service.register(new RegisterCommand("Learner@Example.com", "correct horse battery", "Lan"));

        assertThat(result.user().email()).isEqualTo("learner@example.com");
        assertThat(result.user().firstName()).isEqualTo("Lan");
        assertThat(result.user().role()).isEqualTo(UserRole.CUSTOMER);
        assertThat(result.token()).isNotBlank();
        assertThat(users.saved.passwordHash()).startsWith("pbkdf2$").doesNotContain("correct horse battery");
        assertThat(service.me(result.token()).user()).isEqualTo(result.user());
    }

    @Test
    void rejectsDuplicateEmailWithoutRevealingStoredUser() {
        service.register(new RegisterCommand("learner@example.com", "first password", "Lan"));

        assertThatThrownBy(() -> service.register(new RegisterCommand("LEARNER@example.com", "second password", "Other")))
                .isInstanceOf(AuthException.class)
                .extracting("code").isEqualTo("AUTH_EMAIL_EXISTS");
    }

    @Test
    void loginAndLogoutInvalidateOpaqueSession() {
        service.register(new RegisterCommand("learner@example.com", "correct password", "Lan"));

        AuthSessionResponse login = service.login(new LoginCommand("LEARNER@example.com", "correct password"));
        assertThat(service.me(login.token()).user().email()).isEqualTo("learner@example.com");

        service.logout(login.token());

        assertThatThrownBy(() -> service.me(login.token()))
                .isInstanceOf(AuthException.class)
                .extracting("code").isEqualTo("AUTH_UNAUTHORIZED");
    }

    private static final class FakeUserRepository implements AuthUserRepository {
        private AuthUser saved;

        @Override public AuthUser findByEmail(String email) { return saved != null && saved.email().equals(email) ? saved : null; }
        @Override public AuthUser findById(UUID id) { return saved != null && saved.id().equals(id) ? saved : null; }
        @Override public AuthUser save(AuthUser user) { saved = user; return user; }
    }

    private static final class FakeSessionRepository implements AuthSessionRepository {
        private final List<AuthSession> saved = new ArrayList<>();

        @Override public AuthSession save(AuthSession session) { saved.add(session); return session; }
        @Override public AuthSession findActiveByTokenHash(String tokenHash, Instant now) {
            return saved.stream().filter(session -> session.tokenHash().equals(tokenHash)
                    && session.revokedAt() == null && session.expiresAt().isAfter(now)).findFirst().orElse(null);
        }
        @Override public void revoke(String tokenHash, Instant revokedAt) {
            for (int i = 0; i < saved.size(); i++) {
                AuthSession current = saved.get(i);
                if (current.tokenHash().equals(tokenHash)) {
                    saved.set(i, new AuthSession(current.id(), current.userId(), current.tokenHash(), current.expiresAt(), current.createdAt(), revokedAt));
                }
            }
        }
    }
}
