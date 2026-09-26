package com.ieltsaitutor.preferences;

import java.time.Instant;

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
        Long version,
        Instant updatedAt) {

    public static UserPreferences defaults(Instant updatedAt) {
        return new UserPreferences("SYSTEM", "GOLD", "DEFAULT", "DEFAULT", "SYSTEM",
                false, true, false, 40, 40, 0L, updatedAt);
    }

    public UserPreferences withVersion(long newVersion, Instant newUpdatedAt) {
        return new UserPreferences(themeMode, accentPreset, fontScale, density, reduceMotion,
                proactiveAiEnabled, crossHighlightEnabled, timerDefaultEnabled,
                readingSplitRatio, writingSplitRatio, newVersion, newUpdatedAt);
    }
}
