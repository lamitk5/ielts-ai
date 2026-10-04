package com.ieltsaitutor.database;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class LegacySchemaCompatibilityTest {

    @Test
    void preservationToolingExistsAndContainsNonDestructiveGuards() {
        Path repo = Path.of(System.getProperty("user.dir")).toAbsolutePath().getParent();
        Path script = repo.resolve("scripts/db/rehearse-legacy-remediation.ps1");
        assertTrue(Files.exists(script), "legacy rehearsal script must exist");
        String contents;
        try {
            contents = Files.readString(script);
        } catch (Exception error) {
            throw new AssertionError("legacy rehearsal script must be readable", error);
        }
        assertTrue(contents.contains("DO NOT DROP"));
        assertTrue(contents.contains("REHEARSAL_ONLY"));
        assertTrue(contents.contains("flyway_schema_history"));
    }

    @Test
    void migrationInspectionDoesNotPermitBlindHistoryMutation() throws Exception {
        Path repo = Path.of(System.getProperty("user.dir")).toAbsolutePath().getParent();
        Path script = repo.resolve("scripts/db/inspect-flyway-state.ps1");
        assertTrue(Files.exists(script), "migration inspection script must exist");
        String contents = Files.readString(script);
        assertTrue(contents.contains("information_schema"));
        assertTrue(contents.contains("flyway_schema_history"));
        assertTrue(contents.contains("DO NOT EDIT"));
    }
}
