package com.ieltsaitutor.submission;

public record SubmissionStartCommand(String publishedSetId, String skill, String idempotencyKey) {}
