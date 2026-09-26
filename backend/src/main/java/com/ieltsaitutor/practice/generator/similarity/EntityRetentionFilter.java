package com.ieltsaitutor.practice.generator.similarity;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class EntityRetentionFilter {

    private static final Pattern CAPITALIZED_WORDS = Pattern.compile("\\b[A-Z][a-z]{2,}\\b");

    public double calculateEntityRetention(String sourceText, String generatedText) {
        if (sourceText == null || generatedText == null || sourceText.isBlank() || generatedText.isBlank()) {
            return 0.0;
        }

        Set<String> sourceEntities = extractCapitalizedEntities(sourceText);
        if (sourceEntities.isEmpty()) return 0.0;

        Set<String> generatedEntities = extractCapitalizedEntities(generatedText);

        int retained = 0;
        for (String entity : sourceEntities) {
            if (generatedEntities.contains(entity)) {
                retained++;
            }
        }
        return (double) retained / sourceEntities.size();
    }

    private Set<String> extractCapitalizedEntities(String text) {
        Set<String> entities = new HashSet<>();
        Matcher matcher = CAPITALIZED_WORDS.matcher(text);
        while (matcher.find()) {
            String word = matcher.group();
            // Filter common English sentence starters
            if (!isCommonStopword(word)) {
                entities.add(word);
            }
        }
        return entities;
    }

    private boolean isCommonStopword(String word) {
        String lower = word.toLowerCase();
        return Set.of("the", "this", "that", "these", "those", "there", "their", "when", "while", "where", "what", "which", "however", "furthermore", "moreover", "consequently", "although", "because").contains(lower);
    }
}
