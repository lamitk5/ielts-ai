package com.ielts.tutor.ai;

import com.ielts.tutor.infrastructure.InfrastructureService;
import com.ielts.tutor.shared.SharedComponent;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final InfrastructureService infrastructureService;
    private final SharedComponent sharedComponent;

    public AiService(InfrastructureService infrastructureService, SharedComponent sharedComponent) {
        this.infrastructureService = infrastructureService;
        this.sharedComponent = sharedComponent;
    }
}
