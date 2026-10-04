package com.ieltsaitutor.learning.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class WeaknessStrengthAnalyzerTest {
    @Test
    void oneMistakeRemainsObservationAndThreeRecurringAttemptsConfirmWeakness() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        WeaknessStrengthAnalyzer analyzer = new WeaknessStrengthAnalyzer();
        List<MistakeRecord> one = List.of(mistake(userId, UUID.randomUUID(), now));
        List<MistakeRecord> recurring = List.of(mistake(userId, UUID.randomUUID(), now.minusSeconds(10)),
                mistake(userId, UUID.randomUUID(), now.minusSeconds(20)), mistake(userId, UUID.randomUUID(), now));

        assertEquals(EvidenceState.OBSERVATION, analyzer.analyze(userId, one, now).get(0).evidenceState());
        assertEquals(EvidenceState.CONFIRMED, analyzer.analyze(userId, recurring, now).get(0).evidenceState());
    }

    private MistakeRecord mistake(UUID userId, UUID attemptId, Instant at) {
        return new MistakeRecord(UUID.randomUUID(), userId, Skill.READING, "set", attemptId, "q", "DETAIL", "A", null,
                "DETAIL_MISREAD", MistakeMethod.DETERMINISTIC, 1d, "DETAIL", "A", at, null, MistakeStatus.OPEN, 1);
    }
}
