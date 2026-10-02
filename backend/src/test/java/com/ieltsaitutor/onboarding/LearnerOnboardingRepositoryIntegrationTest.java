package com.ieltsaitutor.onboarding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.ieltsaitutor.auth.AuthException;
import com.ieltsaitutor.learning.intelligence.Skill;

/**
 * Round-trips the onboarding record through a real PostgreSQL NUMERIC column so
 * a target band such as 6.5 cannot silently fail to map back to a Double.
 *
 * <p>Uses the same isolated scratch database contract as the Flyway chain test.
 */
class LearnerOnboardingRepositoryIntegrationTest {

    private static final String ADMIN_URL = property("phase5.flyway.admin-url", "PHASE5_FLYWAY_ADMIN_URL");
    private static final String USER = property("phase5.flyway.user", "PHASE5_FLYWAY_USER");
    private static final String PASSWORD = property("phase5.flyway.password", "PHASE5_FLYWAY_PASSWORD");

    private String database;
    private String dbUrl;
    private LearnerOnboardingService service;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(ADMIN_URL != null,
                "DATABASE_RUNTIME_TEST = BLOCKED - supply -Dphase5.flyway.admin-url");
        database = "phase5_onboarding_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        dbUrl = ADMIN_URL.substring(0, ADMIN_URL.lastIndexOf('/') + 1) + database;
        try (Connection connection = DriverManager.getConnection(ADMIN_URL, USER, PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        Flyway.configure().dataSource(dbUrl, USER, PASSWORD)
                .locations("classpath:db/migration").outOfOrder(true).load().migrate();

        DataSource dataSource = new DriverManagerDataSource(dbUrl, USER, PASSWORD);
        service = new LearnerOnboardingService(new JdbcLearnerOnboardingRepository(
                new NamedParameterJdbcTemplate(dataSource)));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database == null) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(ADMIN_URL, USER, PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + database + " WITH (FORCE)");
        }
    }

    @Test
    void decimalTargetBandSurvivesThePostgresRoundTrip() throws Exception {
        UUID userId = createUser();
        for (double band : new double[] { 0.0, 0.5, 5.5, 6.5, 7.0, 9.0 }) {
            service.save(userId, new OnboardingGoalCommand("INTERMEDIATE", band, null, Skill.READING,
                    45, 5, OnboardingState.COMPLETED), versionAfterFirstSave(userId, band));
        }

        LearnerOnboardingProfile reloaded = service.get(userId);
        assertEquals(9.0, reloaded.targetBand(), "numeric target band must round-trip exactly");
        assertEquals(Skill.READING, reloaded.perceivedWeakestSkill());
        assertEquals(45, reloaded.dailyStudyMinutes());
        assertEquals(5, reloaded.studyDaysPerWeek());
        assertEquals("SELF_REPORTED", reloaded.source());
        assertNull(reloaded.measuredLevel(), "self-report must never gain a measured level");
        assertNull(reloaded.evidenceReference(), "self-report must never gain an evidence reference");
    }

    @Test
    void aFreshLearnerHasNoGoalsInvented() throws Exception {
        UUID userId = createUser();

        LearnerOnboardingProfile profile = service.get(userId);

        assertEquals(OnboardingState.NOT_STARTED, profile.state());
        assertNull(profile.selfReportedLevel());
        assertNull(profile.targetBand());
        assertNull(profile.perceivedWeakestSkill());
        assertNull(profile.dailyStudyMinutes());
        assertEquals("SELF_REPORTED", profile.source());
    }

    @Test
    void staleVersionIsRejectedAndRepeatedSkipDoesNotAdvanceTheVersion() throws Exception {
        UUID userId = createUser();

        LearnerOnboardingProfile skipped = service.complete(userId, OnboardingState.SKIPPED, 0);
        LearnerOnboardingProfile again = service.complete(userId, OnboardingState.SKIPPED, skipped.version());
        assertEquals(skipped.version(), again.version(), "repeated skip must be idempotent");

        AuthException stale = assertThrows(AuthException.class,
                () -> service.save(userId, new OnboardingGoalCommand("BEGINNER", 5.0, null, Skill.WRITING,
                        30, 3, OnboardingState.COMPLETED), 0));
        assertEquals("ONBOARDING_VERSION_CONFLICT", stale.code());
    }

    private long versionAfterFirstSave(UUID userId, double band) {
        return service.get(userId).version();
    }

    private UUID createUser() throws Exception {
        UUID userId = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(dbUrl, USER, PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO app_users(id, email_normalized, first_name, password_hash, role, created_at)"
                    + " VALUES ('" + userId + "', '" + userId + "@example.com', 'Round', 'x', 'CUSTOMER',"
                    + " CURRENT_TIMESTAMP)");
        }
        return userId;
    }

    private static String property(String systemProperty, String environmentVariable) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable);
        }
        return value == null || value.isBlank() ? null : value;
    }
}