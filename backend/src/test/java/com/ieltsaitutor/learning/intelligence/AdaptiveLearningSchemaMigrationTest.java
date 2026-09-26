package com.ieltsaitutor.learning.intelligence;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class AdaptiveLearningSchemaMigrationTest {
    @Test
    void adaptiveSchemaDeclaresOwnershipAndReplayConstraints() throws Exception {
        var resource = getClass().getResourceAsStream("/db/migration/V9__create_adaptive_learning_core.sql");
        assertTrue(resource != null, "adaptive migration must be present in the coordinated V9 slot");
        String sql = new String(resource.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
        assertTrue(sql.contains("user_id"));
        assertTrue(sql.contains("client_event_id"));
        assertTrue(sql.contains("learning_events"));
        assertTrue(sql.contains("student_learning_profiles"));
        assertTrue(sql.contains("learning_roadmaps"));
    }
}
