package com.ieltsaitutor.practice;

import java.util.Map;
import java.util.UUID;

public interface PracticeAttemptStore {
    void save(UUID userId, String skill, String setId, int score, int total, Map<String, String> answers);
}
