package com.ielts.tutor.domain;

import com.ielts.tutor.ai.api.AiEvaluator;
import com.ielts.tutor.infrastructure.InfrastructureService;
import com.ielts.tutor.shared.SharedComponent;
import org.springframework.stereotype.Service;

@Service
public class DomainService {

    private final AiEvaluator aiEvaluator;
    private final InfrastructureService infrastructureService;
    private final SharedComponent sharedComponent;

    public DomainService(AiEvaluator aiEvaluator,
                         InfrastructureService infrastructureService,
                         SharedComponent sharedComponent) {
        this.aiEvaluator = aiEvaluator;
        this.infrastructureService = infrastructureService;
        this.sharedComponent = sharedComponent;
    }
}
