package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TutorAttachmentSchemaContractTest {
    private static final Path MIGRATION = Path.of("src/main/resources/db/migration/V30__create_tutor_attachment_schema.sql");

    @Test
    void schemaDefinesPrivateAttachmentTables() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains("CREATE TABLE ai_attachments");
        assertThat(sql).contains("CREATE TABLE ai_attachment_chunks");
        assertThat(sql).contains("CREATE TABLE ai_message_attachments");
        assertThat(sql).contains("vector(768)");
    }

    @Test
    void schemaDefinesOwnerConversationStatusIndexes() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains("owner_user_id");
        assertThat(sql).contains("conversation_id");
        assertThat(sql).contains("status");
        assertThat(sql).contains("CREATE INDEX");
    }

    @Test
    void schemaPreservesVectorMetadata() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains("embedding_provider");
        assertThat(sql).contains("embedding_model");
        assertThat(sql).contains("embedding_dimension");
        assertThat(sql).contains("embedding_version");
    }

    @Test
    void schemaDoesNotEditHistoricalMigrations() throws Exception {
        try (var files = Files.list(Path.of("src/main/resources/db/migration"))) {
            assertThat(files.map(path -> path.getFileName().toString())
                    .filter(name -> name.matches("V(?:[1-9]|[12][0-9])__.*\\.sql")))
                    .allMatch(name -> !name.startsWith("V30__"));
        }
    }

    @Test
    void attachmentRecordCarriesConversationAndStorageMetadata() {
        TutorAttachment attachment = new TutorAttachment(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "essay.pdf", "essay.pdf",
                "application/pdf", AttachmentKind.DOCUMENT, 12L, "sha", "user/file", 
                TutorAttachment.AttachmentStatus.STORED, null, null, 0, null, null, null);

        assertThat(attachment.conversationId()).isNotNull();
        assertThat(attachment.storageKey()).isEqualTo("user/file");
        assertThat(attachment.kind()).isEqualTo(AttachmentKind.DOCUMENT);
    }
}
