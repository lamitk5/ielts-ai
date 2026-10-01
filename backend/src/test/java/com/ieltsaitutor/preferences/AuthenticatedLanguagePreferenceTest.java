package com.ieltsaitutor.preferences;

import com.ieltsaitutor.auth.AuthException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedLanguagePreferenceTest {
    @Test
    void authenticatedPreferenceRoundTripPersistsLanguageWithExistingEnvelope() {
        MemoryRepository repository = new MemoryRepository();
        UserPreferencesService service = new UserPreferencesService(repository);
        UUID userId = UUID.randomUUID();
        service.get(userId);

        UserPreferences request = new UserPreferences("DARK", "GOLD", "DEFAULT", "DEFAULT", "SYSTEM",
                false, true, false, 40, 40, "EN", 0L, null);
        UserPreferences saved = service.update(userId, request, 0);

        assertThat(saved.language()).isEqualTo("EN");
        assertThat(service.get(userId).language()).isEqualTo("EN");
        assertThat(saved.themeMode()).isEqualTo("DARK");
    }

    @Test
    void missingLanguageDefaultsToVietnameseAndUnsupportedLanguageIsRejected() {
        MemoryRepository repository = new MemoryRepository();
        UserPreferencesService service = new UserPreferencesService(repository);
        UUID userId = UUID.randomUUID();

        assertThat(service.get(userId).language()).isEqualTo("VI");
        UserPreferences invalid = new UserPreferences("SYSTEM", "GOLD", "DEFAULT", "DEFAULT", "SYSTEM",
                false, true, false, 40, 40, "FR", 0L, null);
        assertThatThrownBy(() -> service.update(userId, invalid, 0))
                .isInstanceOfSatisfying(AuthException.class,
                        error -> assertThat(error.code()).isEqualTo("PREFERENCES_INVALID_REQUEST"));
    }

    private static final class MemoryRepository implements UserPreferencesRepository {
        private final Map<UUID, UserPreferences> rows = new HashMap<>();

        public UserPreferences find(UUID userId) { return rows.get(userId); }
        public void insertDefault(UUID userId) { rows.putIfAbsent(userId, UserPreferences.defaults(Instant.parse("2026-01-01T00:00:00Z"))); }
        public boolean update(UUID userId, UserPreferences value, long expectedVersion) {
            UserPreferences current = rows.get(userId);
            if (current == null || current.version() != expectedVersion) return false;
            rows.put(userId, value.withVersion(expectedVersion + 1, Instant.parse("2026-01-02T00:00:00Z")));
            return true;
        }
    }
}
