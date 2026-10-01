package com.ieltsaitutor.practice;

import java.util.List;

public record PracticeSet(String id, String skill, String title, String description, List<PracticeQuestion> questions, PracticePassage passage) {
    public PracticeSet(String id, String skill, String title, String description, List<PracticeQuestion> questions) {
        this(id, skill, title, description, questions, null);
    }
}
