package com.ieltsaitutor.submission;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class SubmissionOwnershipService {
    public PracticeSubmission requireOwner(UUID authenticatedUserId, PracticeSubmission submission) {
        if (authenticatedUserId == null || submission == null || !authenticatedUserId.equals(submission.userId())) {
            throw new SubmissionConflictException("Submission is not available");
        }
        return submission;
    }
}
