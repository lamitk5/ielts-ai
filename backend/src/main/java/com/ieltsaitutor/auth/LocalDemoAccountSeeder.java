package com.ieltsaitutor.auth;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local-demo")
public final class LocalDemoAccountSeeder implements CommandLineRunner {
    private final AuthUserRepository users;
    private final PasswordHasher passwords;
    private final LocalDemoAccountProperties properties;
    private final Clock clock;

    @Autowired
    public LocalDemoAccountSeeder(AuthUserRepository users, PasswordHasher passwords,
            LocalDemoAccountProperties properties) {
        this(users, passwords, properties, Clock.systemUTC());
    }

    LocalDemoAccountSeeder(AuthUserRepository users, PasswordHasher passwords,
            LocalDemoAccountProperties properties, Clock clock) {
        this.users = users;
        this.passwords = passwords;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        seed(properties.studentEmail(), properties.studentFirstName(), properties.studentPassword(), UserRole.CUSTOMER);
        seed(properties.adminEmail(), properties.adminFirstName(), properties.adminPassword(), UserRole.ADMIN);
    }

    private void seed(String rawEmail, String rawFirstName, String password, UserRole expectedRole) {
        String email = normalize(rawEmail);
        if (email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("Local demo credentials must be configured for both accounts.");
        }
        AuthUser existing = users.findByEmail(email);
        if (existing != null) {
            if (existing.role() != expectedRole) {
                throw new IllegalStateException("Local demo account has an unexpected role for " + email + ".");
            }
            return;
        }
        String firstName = rawFirstName == null || rawFirstName.isBlank() ? expectedRole.name() : rawFirstName.trim();
        users.save(new AuthUser(UUID.randomUUID(), email, firstName, passwords.hash(password), expectedRole, clock.instant()));
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
