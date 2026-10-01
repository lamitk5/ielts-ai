package com.ieltsaitutor.rag.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.domain.Skill;

public class RagManifestParser {
    private final Path filesRoot;

    public RagManifestParser(Path filesRoot) { this.filesRoot = filesRoot.toAbsolutePath().normalize(); }

    public RagManifest parse(Path manifestPath) {
        try {
            List<Map<String, String>> entries = new ArrayList<>();
            Map<String, String> current = null;
            for (String raw : Files.readAllLines(manifestPath)) {
                String line = raw.trim();
                if (line.isBlank() || line.equals("sources:")) continue;
                if (line.startsWith("- ")) {
                    if (current != null) entries.add(current);
                    current = new HashMap<>();
                    line = line.substring(2).trim();
                }
                if (current == null || !line.contains(":")) throw new IllegalArgumentException("Invalid manifest entry");
                int separator = line.indexOf(':');
                current.put(line.substring(0, separator).trim(), unquote(line.substring(separator + 1).trim()));
            }
            if (current != null) entries.add(current);
            if (entries.isEmpty()) throw new IllegalArgumentException("Manifest has no sources");
            return new RagManifest(entries.stream().map(this::toEntry).toList());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Manifest cannot be read", exception);
        }
    }

    private RagManifestEntry toEntry(Map<String, String> values) {
        String relative = required(values, "file");
        Path file = filesRoot.resolve(relative).normalize();
        if (!file.startsWith(filesRoot) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Manifest file must be a regular file below the files root");
        }
        String rightsNote = required(values, "rightsNote");
        Skill skill;
        RightsStatus rightsStatus;
        try {
            skill = Skill.valueOf(required(values, "skill").toUpperCase());
            rightsStatus = RightsStatus.valueOf(required(values, "rightsStatus").toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Manifest skill or rights status is unsupported", exception);
        }
        return new RagManifestEntry(file, required(values, "title"), values.get("author"), values.get("organization"),
                values.getOrDefault("version", "1"), required(values, "language"), skill, rightsStatus, rightsNote,
                values.getOrDefault("sourceType", "CLI_MANIFEST"));
    }

    private String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Manifest field is required: " + key);
        return value;
    }

    private String unquote(String value) {
        if (value.length() >= 2 && ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\"")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
