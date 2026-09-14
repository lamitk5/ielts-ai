package com.ielts.tutor.ai.nlp.speaking;

import com.ielts.tutor.ai.asr.AsrService;
import com.ielts.tutor.shared.SharedComponent;
import org.springframework.stereotype.Service;

@Service
public class SpeakingService {

    private final SpeakingEvaluator speakingEvaluator;
    private final AsrService asrService;
    private final SharedComponent sharedComponent;

    public SpeakingService(SpeakingEvaluator speakingEvaluator, AsrService asrService, SharedComponent sharedComponent) {
        this.speakingEvaluator = speakingEvaluator;
        this.asrService = asrService;
        this.sharedComponent = sharedComponent;
    }
}
