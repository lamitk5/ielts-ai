package com.ieltsaitutor.practice.generator.domain;

public enum GenerationState {
    DRAFT,
    GENERATING,
    AUTO_VALIDATING,
    PENDING_REVIEW,
    APPROVED,
    NEEDS_REVISION,
    REJECTED
}
