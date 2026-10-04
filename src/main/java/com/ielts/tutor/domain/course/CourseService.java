package com.ielts.tutor.domain.course;

import com.ielts.tutor.domain.user.UserService;
import com.ielts.tutor.shared.SharedComponent;
import org.springframework.stereotype.Service;

@Service
public class CourseService {

    private final UserService userService;
    private final SharedComponent sharedComponent;

    public CourseService(UserService userService, SharedComponent sharedComponent) {
        this.userService = userService;
        this.sharedComponent = sharedComponent;
    }
}
