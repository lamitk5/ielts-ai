package com.ieltsaitutor.learning;

import java.util.List;
import java.util.UUID;

public interface LearningRepository {
    void saveAttempt(LearningAttempt attempt);
    List<LearningAttempt> findAttempts(UUID userId);
    void saveActivity(LearningActivity activity);
    List<LearningActivity> findActivities(UUID userId);
}
