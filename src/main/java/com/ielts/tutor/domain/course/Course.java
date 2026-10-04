package com.ielts.tutor.domain.course;

import com.ielts.tutor.domain.user.User;
import java.util.UUID;

public class Course {

    private UUID id;
    private String title;
    private String description;
    private User instructor;

    public Course() {
    }

    public Course(UUID id, String title, String description, User instructor) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.instructor = instructor;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getInstructor() {
        return instructor;
    }

    public void setInstructor(User instructor) {
        this.instructor = instructor;
    }
}
