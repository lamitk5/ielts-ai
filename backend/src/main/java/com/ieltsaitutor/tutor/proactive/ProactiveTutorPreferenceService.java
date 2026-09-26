package com.ieltsaitutor.tutor.proactive;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ieltsaitutor.preferences.UserPreferencesService;

@Service
public class ProactiveTutorPreferenceService {
    private final UserPreferencesService preferences;

    public ProactiveTutorPreferenceService(UserPreferencesService preferences) {
        this.preferences = preferences;
    }

    public boolean enabled(UUID userId) {
        return userId != null && Boolean.TRUE.equals(preferences.get(userId).proactiveAiEnabled());
    }
}
