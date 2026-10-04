package com.ielts.tutor.domain.user;

import com.ielts.tutor.shared.SharedComponent;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final SharedComponent sharedComponent;

    public UserService(SharedComponent sharedComponent) {
        this.sharedComponent = sharedComponent;
    }
}
