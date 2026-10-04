package com.ielts.tutor.domain.progress;

import com.ielts.tutor.domain.course.CourseService;
import com.ielts.tutor.domain.lesson.LessonService;
import com.ielts.tutor.domain.skill.SkillService;
import com.ielts.tutor.domain.user.UserService;
import com.ielts.tutor.shared.SharedComponent;
import org.springframework.stereotype.Service;

@Service
public class ProgressService {

    private final UserService userService;
    private final CourseService courseService;
    private final LessonService lessonService;
    private final SkillService skillService;
    private final SharedComponent sharedComponent;

    public ProgressService(UserService userService,
                           CourseService courseService,
                           LessonService lessonService,
                           SkillService skillService,
                           SharedComponent sharedComponent) {
        this.userService = userService;
        this.courseService = courseService;
        this.lessonService = lessonService;
        this.skillService = skillService;
        this.sharedComponent = sharedComponent;
    }
}
