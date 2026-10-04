package com.ieltsaitutor.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the full additive Flyway chain applies on a clean isolated database
 * and that a second application startup is a safe no-op.
 *
 * <p>Point at a scratch database with {@code -Dphase5.flyway.admin-url} and
 * {@code -Dphase5.flyway.url} (or the matching environment variables). A fresh
 * database is created and dropped around each test, so the check never touches
 * a learner or legacy database. The test skips when no URL is supplied.
 */
class FlywayChainIntegrationTest {

    private static final String ADMIN_URL = property("phase5.flyway.admin-url", "PHASE5_FLYWAY_ADMIN_URL");
    private static final String USER = property("phase5.flyway.user", "PHASE5_FLYWAY_USER");
    private static final String PASSWORD = property("phase5.flyway.password", "PHASE5_FLYWAY_PASSWORD");

    private String database;
    private String dbUrl;

    @BeforeEach
    void createScratchDatabase() throws Exception {
        assumeTrue(ADMIN_URL != null,
                "DATABASE_RUNTIME_TEST = BLOCKED - supply -Dphase5.flyway.admin-url");
        database = "phase5_chain_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        dbUrl = ADMIN_URL.substring(0, ADMIN_URL.lastIndexOf('/') + 1) + database;
        try (Connection connection = DriverManager.getConnection(ADMIN_URL, USER, PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
    }

    @AfterEach
    void dropScratchDatabase() throws Exception {
        if (database == null) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(ADMIN_URL, USER, PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + database + " WITH (FORCE)");
        }
    }

    @Test
    void fullChainAppliesOnceAndSecondStartupIsANoOp() {
        Flyway flyway = flyway();
        flyway.migrate();

        List<String> firstRun = appliedVersions();
        assertTrue(firstRun.size() > 1, "first migrate must apply the whole chain");
        assertEquals(0, flyway.info().pending().length, "first migrate must apply every pending script");

        int appliedOnSecondStartup = flyway.migrate().migrationsExecuted;
        assertEquals(0, appliedOnSecondStartup, "second startup must be a no-op");
        assertEquals(firstRun, appliedVersions(), "second startup must not renumber migrations");
        assertTrue(firstRun.stream().anyMatch(entry -> entry.startsWith("44:")),
                "phase 5 onboarding migration must be part of the chain");
    }

    @Test
    void phaseFiveTablesExistWithoutDestroyingPhaseTwoAndPhaseFourTables() throws Exception {
        flyway().migrate();

        try (Connection connection = DriverManager.getConnection(dbUrl, USER, PASSWORD);
                Statement statement = connection.createStatement()) {
            assertTrue(tableExists(statement, "learner_onboarding_profiles"),
                    "phase 5 onboarding table must exist after migrate");
            assertTrue(columnExists(statement, "learner_onboarding_profiles", "evidence_reference"));
            assertTrue(tableExists(statement, "learning_mistakes"),
                    "phase 2 evidence table must survive the additive chain");
            assertTrue(tableExists(statement, "student_learning_profiles"),
                    "phase 2 adaptive profile must survive the additive chain");
            assertTrue(tableExists(statement, "practice_submissions"),
                    "phase 4 canonical submissions must survive the additive chain");
            assertTrue(tableExists(statement, "submission_reviews"),
                    "phase 4 admin review table must survive the additive chain");
        }
    }

    @Test
    void learnerOnboardingSelfReportCannotCarryMeasuredEvidence() throws Exception {
        flyway().migrate();
        try (Connection connection = DriverManager.getConnection(dbUrl, USER, PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO app_users(id, email_normalized, first_name, password_hash, role, created_at)
                    VALUES ('11111111-1111-1111-1111-111111111111', 'flyway@example.com', 'Flyway', 'x', 'CUSTOMER',
                            CURRENT_TIMESTAMP)
                    """);

            String failure = null;
            try {
                statement.execute("""
                        INSERT INTO learner_onboarding_profiles(user_id, self_reported_level,
                            perceived_weakest_skill, state, source, basis, evidence_reference, measured_level)
                        VALUES ('11111111-1111-1111-1111-111111111111', 'BEGINNER', 'READING', 'COMPLETED',
                            'SELF_REPORTED', 'LEARNER_DECLARATION', 'fabricated-reference', 'ADVANCED')
                        """);
            } catch (Exception rejected) {
                failure = rejected.getMessage();
            }
            assertTrue(failure != null && failure.toLowerCase().contains("learner_onboarding_self_report_not_evidence"),
                    "database must reject self-report rows carrying measured evidence, got: " + failure);
        }
    }

    @Test
    void everyConfiguredBandStepIsAcceptedAndOutOfRangeBandsAreRejected() throws Exception {
        flyway().migrate();
        try (Connection connection = DriverManager.getConnection(dbUrl, USER, PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO app_users(id, email_normalized, first_name, password_hash, role, created_at)
                    VALUES ('22222222-2222-2222-2222-222222222222', 'bands@example.com', 'Bands', 'x', 'CUSTOMER',
                            CURRENT_TIMESTAMP)
                    """);
            for (double band = 0.0; band <= 9.0; band += 0.5) {
                statement.execute("""
                        INSERT INTO learner_onboarding_profiles(user_id, target_band, state)
                        VALUES ('22222222-2222-2222-2222-222222222222', """ + band
                        + ", 'COMPLETED') ON CONFLICT (user_id) DO UPDATE SET target_band=EXCLUDED.target_band");
            }

            String failure = null;
            try {
                statement.execute("""
                        INSERT INTO app_users(id, email_normalized, first_name, password_hash, role, created_at)
                        VALUES ('33333333-3333-3333-3333-333333333333', 'band@x.com', 'Band', 'x', 'CUSTOMER', CURRENT_TIMESTAMP)
                        """);
                statement.execute("""
                        INSERT INTO learner_onboarding_profiles(user_id, target_band, state)
                        VALUES ('33333333-3333-3333-3333-333333333333', 12.0, 'COMPLETED')
                        """);
            } catch (Exception rejected) {
                failure = rejected.getMessage();
            }
            assertTrue(failure != null && failure.toLowerCase().contains("target_band"),
                    "database must reject an out-of-range target band, got: " + failure);
        }
    }

    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(dbUrl, USER, PASSWORD)
                .locations("classpath:db/migration")
                .outOfOrder(true)
                .load();
    }

    private List<String> appliedVersions() {
        List<String> versions = new ArrayList<>();
        for (var info : flyway().info().all()) {
            if (info.getState().isApplied()) {
                versions.add(info.getVersion() + ":" + info.getDescription());
            }
        }
        versions.sort(String::compareTo);
        return versions;
    }

    private boolean tableExists(Statement statement, String table) throws Exception {
        try (ResultSet rs = statement.executeQuery("SELECT to_regclass('public." + table + "') IS NOT NULL")) {
            return rs.next() && rs.getBoolean(1);
        }
    }

    private boolean columnExists(Statement statement, String table, String column) throws Exception {
        try (ResultSet rs = statement.executeQuery("SELECT EXISTS (SELECT 1 FROM information_schema.columns"
                + " WHERE table_name='" + table + "' AND column_name='" + column + "')")) {
            return rs.next() && rs.getBoolean(1);
        }
    }

    private static String property(String systemProperty, String environmentVariable) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable);
        }
        return value == null || value.isBlank() ? null : value;
    }
}