package com.ieltsaitutor.preferences;

import java.util.UUID;

public interface UserPreferencesRepository {
    UserPreferences find(UUID userId);
    void insertDefault(UUID userId);
    boolean update(UUID userId, UserPreferences value, long expectedVersion);
}
