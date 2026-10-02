package com.ieltsaitutor.assessment.objective;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ieltsaitutor.learning.intelligence.LearningEvidencePipeline;
import com.ieltsaitutor.learning.intelligence.Skill;
import com.ieltsaitutor.submission.PracticeSubmission;

@Component
public class ObjectiveResultPublisher {
    private final LearningEvidencePipeline pipeline;

    public ObjectiveResultPublisher(LearningEvidencePipeline pipeline) { this.pipeline = pipeline; }

    public void publish(PracticeSubmission submission, ObjectiveScore score, Instant occurredAt) {
        Skill skill = Skill.valueOf(submission.skill().toUpperCase());
        pipeline.practiceCompleted(submission.userId(), skill, submission.publishedSetId(), submission.id(),
                score.correctCount(), score.totalQuestions(), occurredAt == null ? Instant.now() : occurredAt);
    }
}
