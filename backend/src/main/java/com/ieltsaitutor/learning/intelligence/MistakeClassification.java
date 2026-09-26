package com.ieltsaitutor.learning.intelligence;

public record MistakeClassification(String category, double confidence, MistakeMethod method, String evidenceCode,
        String evidenceText) {}
