package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;
import java.util.UUID;

public class MistakeRecordService {
    private final MistakeClassifier classifier;
    private final MistakeRecordWriter writer;

    public MistakeRecordService(MistakeClassifier classifier, MistakeRecordWriter writer) {
        this.classifier = classifier;
        this.writer = writer;
    }

    public MistakeRecord record(UUID userId, MistakeEvidence evidence, UUID attemptId, String practiceSetId, Instant detectedAt) {
        MistakeClassification classification = classifier.classify(evidence);
        MistakeRecord result = new MistakeRecord(UUID.randomUUID(), userId, evidence.skill(), practiceSetId, attemptId,
                evidence.questionId(), evidence.questionType(), truncate(evidence.learnerAnswer()), null,
                classification.category(), classification.method(), classification.confidence(), classification.evidenceCode(),
                classification.evidenceText(), detectedAt, evidence.correct() ? detectedAt : null,
                evidence.correct() ? MistakeStatus.RESOLVED : classification.method() == MistakeMethod.UNKNOWN ? MistakeStatus.UNKNOWN : MistakeStatus.OPEN, 1);
        writer.save(result);
        return result;
    }

    private String truncate(String value) { return value == null ? null : value.length() > 4000 ? value.substring(0, 4000) : value; }
}
