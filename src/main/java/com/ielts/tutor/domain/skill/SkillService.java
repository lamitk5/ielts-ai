package com.ielts.tutor.domain.skill;

import com.ielts.tutor.domain.lesson.LessonService;
import com.ielts.tutor.shared.SharedComponent;
import org.springframework.stereotype.Service;

@Service
public class SkillService {

    private final LessonService lessonService;
    private final SharedComponent sharedComponent;

    public SkillService(LessonService lessonService, SharedComponent sharedComponent) {
        this.lessonService = lessonService;
        this.sharedComponent = sharedComponent;
    }
}
