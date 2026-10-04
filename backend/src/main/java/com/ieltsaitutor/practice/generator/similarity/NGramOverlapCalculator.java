package com.ieltsaitutor.practice.generator.similarity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class NGramOverlapCalculator {

    public double calculateOverlap(String textA, String textB, int n) {
        if (textA == null || textB == null || textA.isBlank() || textB.isBlank() || n <= 0) {
            return 0.0;
        }

        List<String> tokensA = tokenize(textA);
        List<String> tokensB = tokenize(textB);

        if (tokensA.size() < n || tokensB.size() < n) {
            return 0.0;
        }

        Set<String> nGramsA = extractNGrams(tokensA, n);
        Set<String> nGramsB = extractNGrams(tokensB, n);

        int intersection = 0;
        for (String gram : nGramsA) {
            if (nGramsB.contains(gram)) {
                intersection++;
            }
        }

        return (double) intersection / nGramsA.size();
    }

    private List<String> tokenize(String text) {
        String clean = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] parts = clean.trim().split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String p : parts) {
            if (!p.isBlank()) tokens.add(p);
        }
        return tokens;
    }

    private Set<String> extractNGrams(List<String> tokens, int n) {
        Set<String> grams = new HashSet<>();
        for (int i = 0; i <= tokens.size() - n; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if (j > 0) sb.append(' ');
                sb.append(tokens.get(i + j));
            }
            grams.add(sb.toString());
        }
        return grams;
    }
}
