package com.ieltsaitutor.mock;

public enum MockTestSessionStatus {
    NOT_STARTED,
    IN_PROGRESS,
    PAUSED,
    SUBMITTED,
    COMPLETED,
    EXPIRED;

    public boolean isTerminal() {
        return this == COMPLETED || this == EXPIRED;
    }

    public boolean isMutable() {
        return this == NOT_STARTED || this == IN_PROGRESS || this == PAUSED;
    }
}
