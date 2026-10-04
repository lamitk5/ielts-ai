package com.ielts.tutor.ai.recommendation;

import com.ielts.tutor.shared.SharedComponent;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private final RecommendationEvaluator recommendationEvaluator;
    private final SharedComponent sharedComponent;

    public RecommendationService(RecommendationEvaluator recommendationEvaluator, SharedComponent sharedComponent) {
        this.recommendationEvaluator = recommendationEvaluator;
        this.sharedComponent = sharedComponent;
    }
}
