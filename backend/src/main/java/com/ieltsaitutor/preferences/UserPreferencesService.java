package com.ieltsaitutor.preferences;

import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.auth.AuthException;

@Service
public class UserPreferencesService {
    private static final Set<String> THEMES = Set.of("SYSTEM", "LIGHT", "DARK");
    private static final Set<String> ACCENTS = Set.of("GOLD", "SAPPHIRE", "EMERALD", "BURGUNDY", "VIOLET", "SLATE");
    private static final Set<String> FONT_SCALES = Set.of("SMALL", "DEFAULT", "LARGE");
    private static final Set<String> DENSITIES = Set.of("COMFORTABLE", "DEFAULT", "COMPACT");
    private static final Set<String> MOTION = Set.of("SYSTEM", "REDUCED", "ALLOWED");
    private static final Set<Integer> RATIOS = Set.of(40, 50, 60);
    private static final Set<String> LANGUAGES = Set.of("VI", "EN");

    private final UserPreferencesRepository repository;

    public UserPreferencesService(UserPreferencesRepository repository) { this.repository = repository; }

    public UserPreferences get(UUID userId) {
        UserPreferences existing = repository.find(userId);
        if (existing != null) return existing;
        repository.insertDefault(userId);
        return repository.find(userId);
    }

    public UserPreferences update(UUID userId, UserPreferences value, long expectedVersion) {
        validate(value, expectedVersion);
        get(userId);
        if (!repository.update(userId, value, expectedVersion)) {
            throw new AuthException("VERSION_CONFLICT", HttpStatus.CONFLICT, "Thiết lập đã được thay đổi. Vui lòng tải lại.");
        }
        return repository.find(userId);
    }

    private void validate(UserPreferences value, long expectedVersion) {
        if (value == null || expectedVersion < 0 || value.version() == null || value.version() != expectedVersion
                || !allowed(THEMES, value.themeMode()) || !allowed(ACCENTS, value.accentPreset())
                || !allowed(FONT_SCALES, value.fontScale()) || !allowed(DENSITIES, value.density())
                || !allowed(MOTION, value.reduceMotion()) || value.proactiveAiEnabled() == null
                || value.crossHighlightEnabled() == null || value.timerDefaultEnabled() == null
                || !allowed(RATIOS, value.readingSplitRatio()) || !allowed(RATIOS, value.writingSplitRatio())
                || !allowed(LANGUAGES, value.language())) {
            throw new AuthException("PREFERENCES_INVALID_REQUEST", HttpStatus.BAD_REQUEST, "Thiết lập chưa hợp lệ.");
        }
    }

    private static <T> boolean allowed(Set<T> choices, T value) { return value != null && choices.contains(value); }
}
