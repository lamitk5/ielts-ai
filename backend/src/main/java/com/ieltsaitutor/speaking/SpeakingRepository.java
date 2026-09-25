package com.ieltsaitutor.speaking;

import java.util.List;
import java.util.UUID;

public interface SpeakingRepository {
    void save(SpeakingAttempt attempt);
    default List<SpeakingAttempt> findByUser(UUID userId) { return List.of(); }
    default java.util.Optional<SpeakingAttempt> findByUserAndId(UUID userId, UUID attemptId) {
        return findByUser(userId).stream().filter(item -> item.id().equals(attemptId)).findFirst();
    }
}
