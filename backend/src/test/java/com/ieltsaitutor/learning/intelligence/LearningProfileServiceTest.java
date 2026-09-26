package com.ieltsaitutor.learning.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LearningProfileServiceTest {
    @Test
    void emptyEvidenceReturnsFourInsufficientSkillProfiles() {
        UUID userId = UUID.randomUUID();
        LearningIntelligenceRepository repository = mock(LearningIntelligenceRepository.class);
        when(repository.findEvents(userId)).thenReturn(List.of());
        when(repository.findMistakes(userId)).thenReturn(List.of());

        LearningProfileService service = new LearningProfileService(repository, new WeaknessStrengthAnalyzer(), new TrendAnalyzer());
        assertEquals(4, service.skills(userId).size());
        assertEquals(EvidenceState.INSUFFICIENT_DATA, service.profile(userId).evidenceState());
    }
}
