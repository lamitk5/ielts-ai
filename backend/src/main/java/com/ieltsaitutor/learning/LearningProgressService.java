package com.ieltsaitutor.learning;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class LearningProgressService {
    private static final List<String> SKILLS = List.of("reading", "listening", "writing", "speaking");
    private final LearningRepository repository;

    public LearningProgressService(LearningRepository repository) { this.repository = repository; }

    public MemberProgress progress(UUID userId) {
        List<LearningAttempt> attempts = repository.findAttempts(userId);
        return new MemberProgress(userId, SKILLS.stream().map(skill -> {
            List<LearningAttempt> skillAttempts = attempts.stream().filter(attempt -> attempt.skill().equals(skill)).toList();
            LearningAttempt latest = skillAttempts.isEmpty() ? null : skillAttempts.get(0);
            return new SkillProgress(capitalize(skill), latest == null ? null : band(latest), skillAttempts.size());
        }).toList());
    }

    public List<LearningAttempt> attempts(UUID userId) { return repository.findAttempts(userId); }

    public List<LearningActivity> activities(UUID userId) { return repository.findActivities(userId); }

    public void recordAttempt(UUID userId, LearningAttempt attempt) {
        if (!userId.equals(attempt.userId())) throw new IllegalArgumentException("Attempt owner mismatch");
        repository.saveAttempt(attempt);
        repository.saveActivity(new LearningActivity(UUID.randomUUID(), userId, attempt.skill(), "PRACTICE_COMPLETED",
                attempt.id().toString(), band(attempt), attempt.createdAt()));
    }

    private double band(LearningAttempt attempt) {
        return Math.min(9, Math.round((attempt.score() * 9d / attempt.total()) * 2) / 2d);
    }

    private String capitalize(String value) { return value.substring(0, 1).toUpperCase() + value.substring(1); }
}
