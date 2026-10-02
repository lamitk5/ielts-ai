package com.ieltsaitutor.results;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Resolves result actions from server-owned state; it never trusts client scores or statuses. */
@Service
public class ResultActionService {
    private final LearnerResultService results;

    public ResultActionService(LearnerResultService results) { this.results = results; }

    public List<ResultAction> actions(UUID ownerId, UUID submissionId) {
        try {
            return results.get(ownerId, submissionId).actions();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }
}
