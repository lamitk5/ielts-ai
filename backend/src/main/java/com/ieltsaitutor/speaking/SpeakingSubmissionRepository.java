package com.ieltsaitutor.speaking;

import java.util.Optional;
import java.util.UUID;

public interface SpeakingSubmissionRepository {
    SpeakingSubmission save(SpeakingSubmission submission);
    Optional<SpeakingSubmission> findById(UUID id);
    Optional<SpeakingSubmission> findBySubmissionId(UUID submissionId);
    SpeakingSubmission updateStatus(UUID submissionId, SpeakingSubmissionState status);
}
