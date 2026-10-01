package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ieltsaitutor.practice.attempt.AttemptLearningEventPublisher;
import com.ieltsaitutor.practice.attempt.PracticeAttempt;

@Component
public class DefaultAttemptLearningEventPublisher implements AttemptLearningEventPublisher {
    private final LearningEvidencePipeline pipeline;

    public DefaultAttemptLearningEventPublisher(LearningEvidencePipeline pipeline) { this.pipeline = pipeline; }

    @Override
    public void publish(PracticeAttempt attempt, Map<String, String> answers, int score, int total) {
        Skill skill = Skill.valueOf(attempt.skill().toUpperCase());
        pipeline.practiceCompleted(attempt.userId(), skill, attempt.practiceId(), attempt.id(), score, total, Instant.now());
    }
}
