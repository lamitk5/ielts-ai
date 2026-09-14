package com.ielts.tutor.application;

import com.ielts.tutor.ai.AiService;
import com.ielts.tutor.domain.DomainService;
import com.ielts.tutor.infrastructure.InfrastructureService;
import com.ielts.tutor.shared.SharedComponent;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService {

    private final DomainService domainService;
    private final AiService aiService;
    private final InfrastructureService infrastructureService;
    private final SharedComponent sharedComponent;

    public ApplicationService(DomainService domainService,
                              AiService aiService,
                              InfrastructureService infrastructureService,
                              SharedComponent sharedComponent) {
        this.domainService = domainService;
        this.aiService = aiService;
        this.infrastructureService = infrastructureService;
        this.sharedComponent = sharedComponent;
    }
}
