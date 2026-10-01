package com.ieltsaitutor.submission;

public record ResolvedPracticeVersion(
        String publishedSetId,
        String skill,
        String practiceVersionId,
        int publicationRevision) {}
