package com.ieltsaitutor.learning.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AdaptiveLearningPropertiesTest {
    @Test
    void defaultsMatchApprovedEvidenceThresholds() {
        AdaptiveLearningProperties properties = AdaptiveLearningProperties.defaults();
        assertEquals(30, properties.recentWindowDays());
        assertEquals(3, properties.minimumAttempts());
        assertEquals(10, properties.minimumEvaluatedItems());
        assertEquals(2, properties.emergingMinOccurrences());
        assertEquals(3, properties.confirmedMinOccurrences());
        assertEquals(2, properties.confirmedMinAttempts());
        assertEquals(.20, properties.confirmedRate());
        assertEquals(2, properties.improvementWindows());
    }
}
