package com.ieltsaitutor.practice.generator.similarity;

public record SimilarityPolicy(
        String version,
        double maxNGramOverlapWarn,
        double maxNGramOverlapFail,
        int maxContiguousRunWarn,
        int maxContiguousRunFail,
        double maxEntityRetentionWarn) {

    public static SimilarityPolicy conservativeV1() {
        return new SimilarityPolicy(
                "similarity-policy-v1.0-conservative",
                0.030, // 3.0% 4-gram overlap warning threshold
                0.080, // 8.0% 4-gram overlap fail threshold
                8,     // 8 contiguous matching words warning
                16,    // 16 contiguous matching words fail
                0.20   // 20% named entity retention warning
        );
    }
}
