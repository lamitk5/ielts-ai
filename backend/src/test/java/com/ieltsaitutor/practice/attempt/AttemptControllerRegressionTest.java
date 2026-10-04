package com.ieltsaitutor.practice.attempt;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class AttemptControllerRegressionTest {
    @Test
    void compatibilitySubmitRequestDoesNotExposeClientAuthoritativeResultFields() {
        assertFalse(Arrays.stream(AttemptController.SubmitRequest.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("score")
                        || component.getName().equals("total")
                        || component.getName().equals("resultPayload")));
    }
}
