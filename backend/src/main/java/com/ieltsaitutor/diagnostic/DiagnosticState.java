package com.ieltsaitutor.diagnostic;

/** Lifecycle of a bounded, resumable diagnostic baseline. */
public enum DiagnosticState {
    IN_PROGRESS,
    SUBMITTED,
    COMPLETED,
    SKIPPED
}