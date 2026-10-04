package com.ieltsaitutor.assessment.objective;

import java.util.List;
import java.util.UUID;

public interface QuestionResultRepository {
    List<QuestionResult> saveAll(List<QuestionResult> results);
    List<QuestionResult> findByOwnedSubmission(UUID userId, UUID submissionId);
}
