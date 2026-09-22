package com.ieltsaitutor.learning;

import java.util.List;
import java.util.UUID;

public record MemberProgress(UUID userId, List<SkillProgress> skills) {}
