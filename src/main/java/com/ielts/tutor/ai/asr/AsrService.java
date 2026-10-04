package com.ielts.tutor.ai.asr;

import com.ielts.tutor.shared.SharedComponent;
import org.springframework.stereotype.Service;

@Service
public class AsrService {

    private final AsrEvaluator asrEvaluator;
    private final SharedComponent sharedComponent;

    public AsrService(AsrEvaluator asrEvaluator, SharedComponent sharedComponent) {
        this.asrEvaluator = asrEvaluator;
        this.sharedComponent = sharedComponent;
    }
}
