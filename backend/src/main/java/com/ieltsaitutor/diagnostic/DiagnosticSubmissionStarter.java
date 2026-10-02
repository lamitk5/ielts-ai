package com.ieltsaitutor.diagnostic;

import java.util.UUID;

import com.ieltsaitutor.submission.SubmissionStartCommand;

/**
 * Starts diagnostic sections through the shared Phase 4 submission engine so
 * diagnostic answers are answered, autosaved and scored exactly like practice.
 * Repeating a start with the same idempotency key reuses the same submission.
 */
public interface DiagnosticSubmissionStarter {
    UUID start(UUID ownerId, SubmissionStartCommand command);
}