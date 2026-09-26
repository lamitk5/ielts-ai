package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class LearningIntelligenceService {
    private final LearningIntelligenceRepository repository;
    private final RoadmapCompletionService completion;
    private final LearningProfileService profiles;
    private final WeaknessStrengthAnalyzer issues = new WeaknessStrengthAnalyzer();
    private final LearningRoadmapPlanner planner = new LearningRoadmapPlanner();

    public LearningIntelligenceService(LearningIntelligenceRepository repository, RoadmapRepository roadmapRepository) {
        this.repository = repository;
        this.completion = new RoadmapCompletionService(roadmapRepository, item -> { });
        this.profiles = new LearningProfileService(repository, issues, new TrendAnalyzer());
    }

    public StudentLearningProfile profile(UUID userId) { return profiles.profile(userId); }
    public List<StudentSkillProfile> skills(UUID userId) { return profiles.skills(userId); }
    public List<MistakeRecord> mistakes(UUID userId) { return repository.findMistakes(userId); }
    public List<StudentLearningIssue> issues(UUID userId) { return issues.analyze(userId, mistakes(userId), Instant.now()); }
    public LearningRoadmap roadmap(UUID userId) { return planner.plan(userId, issues(userId), Instant.now()); }
    public List<LearningEvent> activity(UUID userId) { return repository.findEvents(userId).stream().limit(50).toList(); }
    public boolean completeRoadmapItem(UUID userId, UUID itemId) {
        try { completion.complete(userId, itemId); return true; }
        catch (SecurityException | java.util.NoSuchElementException ex) { return false; }
    }
}
