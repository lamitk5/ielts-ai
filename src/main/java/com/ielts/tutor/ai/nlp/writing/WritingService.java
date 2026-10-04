package com.ielts.tutor.ai.nlp.writing;

import com.ielts.tutor.shared.SharedComponent;
import org.springframework.stereotype.Service;

@Service
public class WritingService {

    private final WritingEvaluator writingEvaluator;
    private final SharedComponent sharedComponent;

    public WritingService(WritingEvaluator writingEvaluator, SharedComponent sharedComponent) {
        this.writingEvaluator = writingEvaluator;
        this.sharedComponent = sharedComponent;
    }
}
