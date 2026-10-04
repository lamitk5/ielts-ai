package com.ielts.tutor.domain.lesson;

import com.ielts.tutor.domain.course.CourseService;
import com.ielts.tutor.shared.SharedComponent;
import org.springframework.stereotype.Service;

@Service
public class LessonService {

    private final CourseService courseService;
    private final SharedComponent sharedComponent;

    public LessonService(CourseService courseService, SharedComponent sharedComponent) {
        this.courseService = courseService;
        this.sharedComponent = sharedComponent;
    }
}
