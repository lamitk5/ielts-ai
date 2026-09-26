package com.ieltsaitutor.learning.intelligence;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RoadmapCompletionServiceTest {
    @Test
    void foreignItemCannotBeCompleted() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        RoadmapRepository repository = mock(RoadmapRepository.class);
        when(repository.findItem(other, itemId)).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () -> new RoadmapCompletionService(repository, event -> {})
                .complete(other, itemId));
    }
}
