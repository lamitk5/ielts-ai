package com.ieltsaitutor.preferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.auth.AuthException;

class UserPreferencesServiceTest {
    private final MemoryRepository repository = new MemoryRepository();
    private final UserPreferencesService service = new UserPreferencesService(repository);

    @Test
    void firstReadCreatesAndReturnsDefaultRecordForOnlyThatUser() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UserPreferences defaults = service.get(first);

        assertThat(defaults.themeMode()).isEqualTo("SYSTEM");
        assertThat(defaults.accentPreset()).isEqualTo("GOLD");
        assertThat(defaults.fontScale()).isEqualTo("DEFAULT");
        assertThat(defaults.density()).isEqualTo("DEFAULT");
        assertThat(defaults.reduceMotion()).isEqualTo("SYSTEM");
        assertThat(defaults.proactiveAiEnabled()).isFalse();
        assertThat(defaults.crossHighlightEnabled()).isTrue();
        assertThat(defaults.timerDefaultEnabled()).isFalse();
        assertThat(defaults.readingSplitRatio()).isEqualTo(40);
        assertThat(defaults.writingSplitRatio()).isEqualTo(40);
        assertThat(defaults.version()).isZero();
        assertThat(defaults.updatedAt()).isNotNull();
        assertThat(service.get(second)).isEqualTo(defaults);
        assertThat(repository.rows).hasSize(2);
    }

    @Test
    void updateReturnsCompleteAuthoritativeRecordAndIncrementsVersion() {
        UUID userId = UUID.randomUUID();
        service.get(userId);
        UserPreferences requested = new UserPreferences("DARK", "EMERALD", "LARGE", "COMFORTABLE", "REDUCED",
                true, false, true, 60, 50, 0L, null);

        UserPreferences saved = service.update(userId, requested, 0);

        assertThat(saved).isEqualTo(repository.find(userId));
        assertThat(saved.themeMode()).isEqualTo("DARK");
        assertThat(saved.accentPreset()).isEqualTo("EMERALD");
        assertThat(saved.density()).isEqualTo("COMFORTABLE");
        assertThat(saved.readingSplitRatio()).isEqualTo(60);
        assertThat(saved.writingSplitRatio()).isEqualTo(50);
        assertThat(saved.version()).isEqualTo(1);
        assertThat(saved.updatedAt()).isNotNull();
    }

    @Test
    void staleVersionCannotOverwriteAnotherUpdate() {
        UUID userId = UUID.randomUUID();
        service.get(userId);
        UserPreferences first = new UserPreferences("LIGHT", "SAPPHIRE", "SMALL", "COMPACT", "ALLOWED",
                false, true, false, 50, 40, 0L, null);
        service.update(userId, first, 0);

        assertThatThrownBy(() -> service.update(userId, first, 0))
                .isInstanceOfSatisfying(AuthException.class, error -> assertThat(error.code()).isEqualTo("VERSION_CONFLICT"));
        assertThat(service.get(userId).themeMode()).isEqualTo("LIGHT");
        assertThat(service.get(userId).version()).isEqualTo(1);
    }

    @Test
    void invalidEnumAndRatioValuesAreRejectedWithoutWriting() {
        UUID userId = UUID.randomUUID();
        UserPreferences valid = new UserPreferences("SYSTEM", "GOLD", "DEFAULT", "DEFAULT", "SYSTEM",
                false, true, false, 40, 40, 0L, null);
        for (UserPreferences invalid : new UserPreferences[] {
                new UserPreferences(null, "GOLD", "DEFAULT", "DEFAULT", "SYSTEM", false, true, false, 40, 40, 0L, null),
                new UserPreferences("BLUE", "GOLD", "DEFAULT", "DEFAULT", "SYSTEM", false, true, false, 40, 40, 0L, null),
                new UserPreferences("SYSTEM", "PINK", "DEFAULT", "DEFAULT", "SYSTEM", false, true, false, 40, 40, 0L, null),
                new UserPreferences("SYSTEM", "GOLD", "HUGE", "DEFAULT", "SYSTEM", false, true, false, 40, 40, 0L, null),
                new UserPreferences("SYSTEM", "GOLD", "DEFAULT", "LOOSE", "SYSTEM", false, true, false, 40, 40, 0L, null),
                new UserPreferences("SYSTEM", "GOLD", "DEFAULT", "DEFAULT", "FAST", false, true, false, 40, 40, 0L, null),
                new UserPreferences("SYSTEM", "GOLD", "DEFAULT", "DEFAULT", "SYSTEM", false, true, false, 45, 40, 0L, null),
                new UserPreferences("SYSTEM", "GOLD", "DEFAULT", "DEFAULT", "SYSTEM", false, true, false, 40, 80, 0L, null),
                new UserPreferences("SYSTEM", "GOLD", "DEFAULT", "DEFAULT", "SYSTEM", false, true, false, null, 40, 0L, null),
                new UserPreferences("SYSTEM", "GOLD", "DEFAULT", "DEFAULT", "SYSTEM", null, true, false, 40, 40, 0L, null)
        }) {
            assertThatThrownBy(() -> service.update(userId, invalid, 0))
                    .isInstanceOfSatisfying(AuthException.class, error -> assertThat(error.code()).isEqualTo("PREFERENCES_INVALID_REQUEST"));
        }
        assertThat(repository.rows).isEmpty();
        assertThat(service.update(userId, valid, 0).version()).isEqualTo(1);
    }

    @Test
    void userRecordsCannotAffectOneAnother() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        service.get(first);
        service.get(second);
        service.update(first, new UserPreferences("DARK", "VIOLET", "DEFAULT", "DEFAULT", "SYSTEM",
                false, true, false, 40, 40, 0L, null), 0);

        assertThat(service.get(second).accentPreset()).isEqualTo("GOLD");
        assertThat(service.get(second).version()).isZero();
    }

    private static final class MemoryRepository implements UserPreferencesRepository {
        private final Map<UUID, UserPreferences> rows = new HashMap<>();

        @Override
        public UserPreferences find(UUID userId) { return rows.get(userId); }

        @Override
        public void insertDefault(UUID userId) { rows.putIfAbsent(userId, UserPreferences.defaults(Instant.parse("2026-01-01T00:00:00Z"))); }

        @Override
        public boolean update(UUID userId, UserPreferences value, long expectedVersion) {
            UserPreferences current = rows.get(userId);
            if (current == null || current.version() != expectedVersion) return false;
            rows.put(userId, value.withVersion(expectedVersion + 1, Instant.parse("2026-01-02T00:00:00Z")));
            return true;
        }
    }
}
