package com.ieltsaitutor.practice.generator.similarity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ContiguousSequenceDetector {

    public int findLongestMatchingSequence(String textA, String textB) {
        if (textA == null || textB == null || textA.isBlank() || textB.isBlank()) {
            return 0;
        }

        List<String> tokensA = tokenize(textA);
        List<String> tokensB = tokenize(textB);

        int maxMatch = 0;
        // Longest Common Substring algorithm on token lists
        int[][] dp = new int[tokensA.size() + 1][tokensB.size() + 1];

        for (int i = 1; i <= tokensA.size(); i++) {
            for (int j = 1; j <= tokensB.size(); j++) {
                if (tokensA.get(i - 1).equalsIgnoreCase(tokensB.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    if (dp[i][j] > maxMatch) {
                        maxMatch = dp[i][j];
                    }
                } else {
                    dp[i][j] = 0;
                }
            }
        }
        return maxMatch;
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
}
