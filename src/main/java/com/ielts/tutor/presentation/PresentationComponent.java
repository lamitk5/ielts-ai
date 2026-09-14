package com.ielts.tutor.presentation;

import com.ielts.tutor.application.ApplicationService;
import com.ielts.tutor.shared.SharedComponent;
import org.springframework.stereotype.Component;

@Component
public class PresentationComponent {

    private final ApplicationService applicationService;
    private final SharedComponent sharedComponent;

    public PresentationComponent(ApplicationService applicationService, SharedComponent sharedComponent) {
        this.applicationService = applicationService;
        this.sharedComponent = sharedComponent;
    }
}
