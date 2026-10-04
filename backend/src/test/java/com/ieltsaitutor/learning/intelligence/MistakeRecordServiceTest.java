package com.ieltsaitutor.learning.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class MistakeRecordServiceTest {
    @Test
    void deterministicCorrectnessRemainsAuthoritative() {
        MistakeRecordWriter writer = mock(MistakeRecordWriter.class);
        MistakeRecordService service = new MistakeRecordService(new DeterministicMistakeClassifier(), writer);
        UUID userId = UUID.randomUUID();

        MistakeRecord result = service.record(userId, new MistakeEvidence(Skill.READING, "DETAIL_MISREAD", "A", "B",
                true, "q1"), UUID.randomUUID(), "set-1", Instant.now());

        assertNotEquals("DETAIL_MISREAD", result.category());
        verify(writer).save(any(MistakeRecord.class));
    }
}
