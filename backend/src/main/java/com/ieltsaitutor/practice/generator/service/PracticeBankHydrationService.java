package com.ieltsaitutor.practice.generator.service;

import java.util.UUID;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeSet;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeVersion;

public interface PracticeBankHydrationService {
    String hydrate(GeneratedPracticeSet set, GeneratedPracticeVersion version, UUID adminId);
}
