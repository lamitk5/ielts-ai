package com.ieltsaitutor.diagnostic;

/**
 * Whether a diagnostic section can be answered honestly.
 *
 * <p>{@link #CAPABILITY_UNAVAILABLE} is a first-class outcome: a skill with no
 * approved content, media, or scoring capability is reported as unavailable
 * rather than simulated with invented data.
 */
public enum DiagnosticAvailability {
    AVAILABLE,
    CAPABILITY_UNAVAILABLE
}