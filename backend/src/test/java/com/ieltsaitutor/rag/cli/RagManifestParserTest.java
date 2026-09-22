package com.ieltsaitutor.rag.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RagManifestParserTest {
    @Test
    void parsesConcreteManifestSchema() throws Exception {
        Path root = Files.createTempDirectory("rag-files");
        Path file = Files.createFile(root.resolve("guide.txt"));
        Path manifest = Files.createTempFile("manifest", ".yml");
        Files.writeString(manifest, "sources:\n  - file: guide.txt\n    title: Guide\n    author: Team\n    organization: Project\n    version: '1'\n    language: en\n    skill: WRITING\n    rightsStatus: PENDING_REVIEW\n    rightsNote: Licensed for project\n    sourceType: PROJECT_CREATED\n");

        RagManifest result = new RagManifestParser(root).parse(manifest);

        assertThat(result.sources()).hasSize(1);
        assertThat(result.sources().getFirst().file()).isEqualTo(file.toAbsolutePath().normalize());
    }

    @Test
    void rejectsMissingRightsNote() throws Exception {
        Path root = Files.createTempDirectory("rag-files");
        Path manifest = Files.createTempFile("manifest", ".yml");
        Files.writeString(manifest, "sources:\n  - file: guide.txt\n    title: Guide\n    language: en\n    skill: GENERAL\n    rightsStatus: PENDING_REVIEW\n");
        assertThatThrownBy(() -> new RagManifestParser(root).parse(manifest)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsPathOutsideFilesRoot() throws Exception {
        Path root = Files.createTempDirectory("rag-files");
        Path manifest = Files.createTempFile("manifest", ".yml");
        Files.writeString(manifest, "sources:\n  - file: ../secret.txt\n    title: Guide\n    language: en\n    skill: GENERAL\n    rightsStatus: PENDING_REVIEW\n    rightsNote: licensed\n");
        assertThatThrownBy(() -> new RagManifestParser(root).parse(manifest)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnsupportedSkill() throws Exception {
        Path root = Files.createTempDirectory("rag-files");
        Path manifest = Files.createTempFile("manifest", ".yml");
        Files.writeString(manifest, "sources:\n  - file: guide.txt\n    title: Guide\n    language: en\n    skill: GRAMMAR\n    rightsStatus: PENDING_REVIEW\n    rightsNote: licensed\n");
        assertThatThrownBy(() -> new RagManifestParser(root).parse(manifest)).isInstanceOf(IllegalArgumentException.class);
    }
}
