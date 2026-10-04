package com.ielts.tutor.domain.skill;

import com.ielts.tutor.domain.lesson.Lesson;
import java.util.UUID;

public class Skill {

    private UUID id;
    private String skillType;
    private Lesson lesson;

    public Skill() {
    }

    public Skill(UUID id, String skillType, Lesson lesson) {
        this.id = id;
        this.skillType = skillType;
        this.lesson = lesson;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSkillType() {
        return skillType;
    }

    public void setSkillType(String skillType) {
        this.skillType = skillType;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }
}
