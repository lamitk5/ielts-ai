package com.ieltsaitutor.tutor.adaptive;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.learning.intelligence.LearningIntelligenceService;

class AdaptiveTutorToolsSecurityTest {
    @Test
    void missingPrincipalCannotReadAdaptiveData() {
        LearningIntelligenceService service = mock(LearningIntelligenceService.class);
        AdaptiveTutorContext context = new AdaptiveTutorContextResolver(service).resolve(null);
        assertTrue(context.issues().isEmpty());
        verifyNoInteractions(service);
    }
}
