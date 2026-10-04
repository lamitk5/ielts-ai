package com.ielts.tutor.domain.progress;

import com.ielts.tutor.domain.course.Course;
import com.ielts.tutor.domain.lesson.Lesson;
import com.ielts.tutor.domain.skill.Skill;
import com.ielts.tutor.domain.user.User;
import java.util.UUID;

public class Progress {

    private UUID id;
    private User user;
    private Course course;
    private Lesson lesson;
    private Skill skill;
    private Double score;
    private boolean completed;

    public Progress() {
    }

    public Progress(UUID id, User user, Course course, Lesson lesson, Skill skill, Double score, boolean completed) {
        this.id = id;
        this.user = user;
        this.course = course;
        this.lesson = lesson;
        this.skill = skill;
        this.score = score;
        this.completed = completed;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
