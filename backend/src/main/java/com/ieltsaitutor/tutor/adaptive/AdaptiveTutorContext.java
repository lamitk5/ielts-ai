package com.ieltsaitutor.tutor.adaptive;

import java.util.List;

import com.ieltsaitutor.learning.intelligence.LearningRoadmap;
import com.ieltsaitutor.learning.intelligence.StudentLearningIssue;
import com.ieltsaitutor.learning.intelligence.StudentLearningProfile;
import com.ieltsaitutor.learning.intelligence.StudentSkillProfile;

/** Server-resolved adaptive context. No client-supplied scores or answer keys enter this record. */
public record AdaptiveTutorContext(StudentLearningProfile profile,
        List<StudentSkillProfile> skills,
        List<StudentLearningIssue> issues,
        LearningRoadmap roadmap) {
    public AdaptiveTutorContext {
        skills = skills == null ? List.of() : List.copyOf(skills);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
