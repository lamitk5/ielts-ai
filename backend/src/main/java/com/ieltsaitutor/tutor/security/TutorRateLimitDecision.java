package com.ieltsaitutor.tutor.security;

public record TutorRateLimitDecision(boolean allowed, int remaining, long retryAfterSeconds) {}
