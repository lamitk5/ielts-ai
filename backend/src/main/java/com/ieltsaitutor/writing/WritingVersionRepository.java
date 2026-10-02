package com.ieltsaitutor.writing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WritingVersionRepository {
    WritingSubmissionVersion save(WritingSubmissionVersion version);
    Optional<WritingSubmissionVersion> findById(UUID id);
    List<WritingSubmissionVersion> findBySubmissionId(UUID submissionId);
    Optional<WritingSubmissionVersion> findBySubmissionIdAndVersionNumber(UUID submissionId, int versionNumber);
    int getNextVersionNumber(UUID submissionId);
}
