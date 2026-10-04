package com.ielts.tutor.domain.lesson;

import com.ielts.tutor.domain.course.Course;
import java.util.UUID;

public class Lesson {

    private UUID id;
    private Course course;
    private String title;
    private int orderIndex;

    public Lesson() {
    }

    public Lesson(UUID id, Course course, String title, int orderIndex) {
        this.id = id;
        this.course = course;
        this.title = title;
        this.orderIndex = orderIndex;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
