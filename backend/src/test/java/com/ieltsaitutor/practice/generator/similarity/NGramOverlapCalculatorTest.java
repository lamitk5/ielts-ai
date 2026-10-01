package com.ieltsaitutor.practice.generator.similarity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NGramOverlapCalculatorTest {

    private NGramOverlapCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new NGramOverlapCalculator();
    }

    @Test
    void calculatesZeroOverlapForCompletelyDifferentTexts() {
        String textA = "Marine biology investigates oceanic flora and fauna across deep sea hydrothermal vents.";
        String textB = "Urban architecture examines skyscraper design principles and structural integrity in earthquakes.";

        double overlap = calculator.calculateOverlap(textA, textB, 4);
        assertEquals(0.0, overlap, 0.001);
    }

    @Test
    void calculatesHighOverlapForNearIdenticalTexts() {
        String textA = "Marine biology investigates oceanic flora and fauna across deep sea hydrothermal vents.";
        String textB = "Marine biology investigates oceanic flora and fauna across deep sea thermal vents.";

        double overlap = calculator.calculateOverlap(textA, textB, 4);
        assertTrue(overlap > 0.4);
    }
}
