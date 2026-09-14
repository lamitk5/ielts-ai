package com.ielts.tutor.infrastructure;

import com.ielts.tutor.shared.SharedComponent;
import org.springframework.stereotype.Service;

@Service
public class InfrastructureService {

    private final SharedComponent sharedComponent;

    public InfrastructureService(SharedComponent sharedComponent) {
        this.sharedComponent = sharedComponent;
    }
}
