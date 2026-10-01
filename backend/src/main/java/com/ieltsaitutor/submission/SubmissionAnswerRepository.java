package com.ieltsaitutor.submission;

import java.util.Optional;
import java.util.UUID;

public interface SubmissionAnswerRepository {
    SubmissionAnswerSnapshot save(SubmissionAnswerSnapshot snapshot);

    Optional<SubmissionAnswerSnapshot> findBySubmissionId(UUID submissionId);
}
