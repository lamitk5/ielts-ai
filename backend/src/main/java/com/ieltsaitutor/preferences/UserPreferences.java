package com.ieltsaitutor.preferences;

import java.time.Instant;
import java.util.Locale;

public record UserPreferences(
        String themeMode,
        String accentPreset,
        String fontScale,
        String density,
        String reduceMotion,
        Boolean proactiveAiEnabled,
        Boolean crossHighlightEnabled,
        Boolean timerDefaultEnabled,
        Integer readingSplitRatio,
        Integer writingSplitRatio,
        String language,
        Long version,
        Instant updatedAt) {

    public UserPreferences(String themeMode, String accentPreset, String fontScale, String density,
            String reduceMotion, Boolean proactiveAiEnabled, Boolean crossHighlightEnabled,
            Boolean timerDefaultEnabled, Integer readingSplitRatio, Integer writingSplitRatio,
            Long version, Instant updatedAt) {
        this(themeMode, accentPreset, fontScale, density, reduceMotion, proactiveAiEnabled,
                crossHighlightEnabled, timerDefaultEnabled, readingSplitRatio, writingSplitRatio,
                "VI", version, updatedAt);
    }

    public UserPreferences {
        language = language == null || language.isBlank() ? "VI" : language.trim().toUpperCase(Locale.ROOT);
    }

    public static UserPreferences defaults(Instant updatedAt) {
        return new UserPreferences("SYSTEM", "GOLD", "DEFAULT", "DEFAULT", "SYSTEM",
                false, true, false, 40, 40, "VI", 0L, updatedAt);
    }

    public UserPreferences withVersion(long newVersion, Instant newUpdatedAt) {
        return new UserPreferences(themeMode, accentPreset, fontScale, density, reduceMotion,
                proactiveAiEnabled, crossHighlightEnabled, timerDefaultEnabled,
                readingSplitRatio, writingSplitRatio, language, newVersion, newUpdatedAt);
    }
}
