package com.ieltsaitutor.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class LocalDemoAccountSeederTest {
    private final PasswordHasher passwords = new PasswordHasher();

    @Test
    void seeds_student_and_admin_with_real_password_hashes_and_roles() {
        FakeUserRepository users = new FakeUserRepository();
        LocalDemoAccountSeeder seeder = new LocalDemoAccountSeeder(users, passwords,
                new LocalDemoAccountProperties("student@example.test", "Demo Student", "student-secret",
                        "admin@example.test", "Demo Admin", "admin-secret"));

        seeder.run();

        AuthUser student = users.findByEmail("student@example.test");
        AuthUser admin = users.findByEmail("admin@example.test");
        assertThat(student.role()).isEqualTo(UserRole.CUSTOMER);
        assertThat(admin.role()).isEqualTo(UserRole.ADMIN);
        assertThat(student.passwordHash()).startsWith("pbkdf2$").doesNotContain("student-secret");
        assertThat(admin.passwordHash()).startsWith("pbkdf2$").doesNotContain("admin-secret");
        assertThat(passwords.matches("student-secret", student.passwordHash())).isTrue();
        assertThat(passwords.matches("admin-secret", admin.passwordHash())).isTrue();
    }

    @Test
    void is_idempotent_and_does_not_replace_existing_credentials() {
        FakeUserRepository users = new FakeUserRepository();
        LocalDemoAccountSeeder seeder = new LocalDemoAccountSeeder(users, passwords,
                new LocalDemoAccountProperties("student@example.test", "Demo Student", "student-secret",
                        "admin@example.test", "Demo Admin", "admin-secret"));

        seeder.run();
        String originalHash = users.findByEmail("student@example.test").passwordHash();
        seeder.run();

        assertThat(users.findByEmail("student@example.test").passwordHash()).isEqualTo(originalHash);
        assertThat(users.saveCount).isEqualTo(2);
    }

    @Test
    void rejects_an_existing_account_with_the_wrong_role() {
        FakeUserRepository users = new FakeUserRepository();
        users.save(new AuthUser(UUID.randomUUID(), "admin@example.test", "Existing", passwords.hash("existing"),
                UserRole.CUSTOMER, Instant.now()));
        LocalDemoAccountSeeder seeder = new LocalDemoAccountSeeder(users, passwords,
                new LocalDemoAccountProperties("student@example.test", "Demo Student", "student-secret",
                        "admin@example.test", "Demo Admin", "admin-secret"));

        assertThatThrownBy(seeder::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("role");
    }

    @Test
    void refuses_to_run_with_missing_demo_credentials() {
        LocalDemoAccountSeeder seeder = new LocalDemoAccountSeeder(new FakeUserRepository(), passwords,
                new LocalDemoAccountProperties("", "Demo Student", "", "admin@example.test", "Demo Admin", "admin-secret"));

        assertThatThrownBy(seeder::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("credentials");
    }

    @Test
    void spring_can_autowire_the_profile_gated_seeder() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("local-demo");
            context.registerBean(AuthUserRepository.class, FakeUserRepository::new);
            context.registerBean(PasswordHasher.class);
            context.registerBean(LocalDemoAccountProperties.class,
                    () -> new LocalDemoAccountProperties("student@example.test", "Student", "student-secret",
                            "admin@example.test", "Admin", "admin-secret"));
            context.registerBean(LocalDemoAccountSeeder.class);

            context.refresh();

            assertThat(context.getBean(LocalDemoAccountSeeder.class)).isNotNull();
        }
    }

    private static final class FakeUserRepository implements AuthUserRepository {
        private final Map<String, AuthUser> users = new HashMap<>();
        private int saveCount;

        @Override public AuthUser findByEmail(String email) { return users.get(email); }
        @Override public AuthUser findById(UUID id) { return users.values().stream().filter(user -> user.id().equals(id)).findFirst().orElse(null); }
        @Override public AuthUser save(AuthUser user) { saveCount++; users.put(user.email(), user); return user; }
    }
}
