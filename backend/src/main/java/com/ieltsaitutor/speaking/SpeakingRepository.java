package com.ieltsaitutor.speaking;

import java.util.List;
import java.util.UUID;

public interface SpeakingRepository {
    void save(SpeakingAttempt attempt);
    default List<SpeakingAttempt> findByUser(UUID userId) { return List.of(); }
}
