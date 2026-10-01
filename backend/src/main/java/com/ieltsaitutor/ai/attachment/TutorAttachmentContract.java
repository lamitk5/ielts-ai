package com.ieltsaitutor.ai.attachment;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TutorAttachmentContract {
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L;
    public static final String VISION_NOT_ENABLED = "VISION_NOT_ENABLED";
    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx", ".txt", ".png", ".jpg", ".jpeg", ".webp");
    public static final Set<String> DISALLOWED_EXTENSIONS = Set.of(
            ".html", ".htm", ".svg", ".exe", ".sh", ".bat", ".cmd", ".js", ".mjs", ".py", ".vbs", ".php"
    );
    public static final Map<String, Set<String>> EXTENSION_MIME_TYPES = Map.of(
            ".pdf", Set.of("application/pdf"),
            ".docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            ".txt", Set.of("text/plain"),
            ".png", Set.of("image/png"),
            ".jpg", Set.of("image/jpeg", "image/jpg"),
            ".jpeg", Set.of("image/jpeg", "image/jpg"),
            ".webp", Set.of("image/webp")
    );

    private TutorAttachmentContract() {
    }

    public static String extensionOf(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.stream().filter(lower::endsWith).findFirst().orElse("");
    }

    public static boolean isImage(String extension) {
        return Set.of(".png", ".jpg", ".jpeg", ".webp").contains(extension);
    }
}
