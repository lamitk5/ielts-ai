package com.ieltsaitutor.speaking;

import java.util.List;
import java.util.UUID;

public interface SpeakingRepository {
    void save(SpeakingAttempt attempt);
    default List<SpeakingAttempt> findByUser(UUID userId) { return List.of(); }
    default java.util.Optional<SpeakingAttempt> findByUserAndId(UUID userId, UUID attemptId) {
        return findByUser(userId).stream().filter(item -> item.id().equals(attemptId)).findFirst();
    }
    default SpeakingAttempt start(UUID userId, String promptId) {
        return new SpeakingAttempt(UUID.randomUUID(), userId, promptId, null, null, "IN_PROGRESS", null, java.time.Instant.now());
    }
    default SpeakingAttempt saveDraft(SpeakingAttempt attempt) { return attempt; }
    default SpeakingAttempt complete(SpeakingAttempt attempt, SpeakingAttempt result) { save(result); return result; }
}
