package com.ieltsaitutor.learning.intelligence;

import java.util.List;
import java.util.UUID;

public interface LearningIntelligenceRepository {
    List<LearningEvent> findEvents(UUID userId);
    List<MistakeRecord> findMistakes(UUID userId);
}
