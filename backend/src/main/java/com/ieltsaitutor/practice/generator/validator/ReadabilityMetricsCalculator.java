package com.ieltsaitutor.practice.generator.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class ReadabilityMetricsCalculator {

    private static final Pattern VOWEL_PATTERN = Pattern.compile("[aeiouy]+", Pattern.CASE_INSENSITIVE);

    public record ReadabilityResult(
            double fleschKincaidGradeLevel,
            double fleschReadingEase,
            int wordCount,
            int sentenceCount,
            int syllableCount) {}

    public ReadabilityResult calculate(String text) {
        if (text == null || text.isBlank()) {
            return new ReadabilityResult(0.0, 0.0, 0, 0, 0);
        }

        String[] sentences = text.split("[.!?]+");
        int sentenceCount = Math.max(1, sentences.length);

        String[] words = text.trim().split("\\s+");
        int wordCount = Math.max(1, words.length);

        int syllableCount = 0;
        for (String word : words) {
            syllableCount += countSyllables(word);
        }
        syllableCount = Math.max(wordCount, syllableCount);

        double wordsPerSentence = (double) wordCount / sentenceCount;
        double syllablesPerWord = (double) syllableCount / wordCount;

        // Standard Flesch-Kincaid Grade Level formula
        double fkGrade = (0.39 * wordsPerSentence) + (11.8 * syllablesPerWord) - 15.59;
        // Standard Flesch Reading Ease formula
        double fre = 206.835 - (1.015 * wordsPerSentence) - (84.6 * syllablesPerWord);

        return new ReadabilityResult(
                Math.round(Math.max(0.0, fkGrade) * 10.0) / 10.0,
                Math.round(Math.max(0.0, Math.min(100.0, fre)) * 10.0) / 10.0,
                wordCount,
                sentenceCount,
                syllableCount
        );
    }

    private int countSyllables(String word) {
        String clean = word.toLowerCase().replaceAll("[^a-z]", "");
        if (clean.length() <= 3) return 1;
        if (clean.endsWith("e") && !clean.endsWith("le") && !clean.endsWith("ee")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        Matcher matcher = VOWEL_PATTERN.matcher(clean);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return Math.max(1, count);
    }
}
