package com.ieltsaitutor.learning.intelligence;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class AdaptiveLearningMigrationCompatibilityTest {
    @Test
    void baseMigrationsRemainPresentAlongsideAdaptiveMigration() {
        for (int version = 1; version <= 9; version++) {
            String resource = version == 9 ? "/db/migration/V9__create_adaptive_learning_core.sql"
                    : "/db/migration/V" + version + "__" + switch (version) {
                        case 1 -> "create_rag_schema.sql";
                        case 2 -> "create_auth_schema.sql";
                        case 3 -> "create_learning_schema.sql";
                        case 4 -> "create_writing_schema.sql";
                        case 5 -> "create_speaking_schema.sql";
                        case 6 -> "add_embedding_space_metadata.sql";
                        case 7 -> "create_user_preferences.sql";
                        default -> "create_learning_drafts.sql";
                    };
            assertNotNull(getClass().getResource(resource), resource);
        }
    }
}
