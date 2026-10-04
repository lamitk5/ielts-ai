package com.ielts.tutor.ai.tts;

import com.ielts.tutor.shared.SharedComponent;
import org.springframework.stereotype.Service;

@Service
public class TtsService {

    private final TtsEvaluator ttsEvaluator;
    private final SharedComponent sharedComponent;

    public TtsService(TtsEvaluator ttsEvaluator, SharedComponent sharedComponent) {
        this.ttsEvaluator = ttsEvaluator;
        this.sharedComponent = sharedComponent;
    }
}
