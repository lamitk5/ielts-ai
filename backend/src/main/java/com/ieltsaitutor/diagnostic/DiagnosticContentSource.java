package com.ieltsaitutor.diagnostic;

import java.util.Optional;

import com.ieltsaitutor.learning.intelligence.Skill;

/** Looks up the newest approved, published content for one diagnostic skill. */
public interface DiagnosticContentSource {
    Optional<DiagnosticContentPin> newestApproved(Skill skill);
}