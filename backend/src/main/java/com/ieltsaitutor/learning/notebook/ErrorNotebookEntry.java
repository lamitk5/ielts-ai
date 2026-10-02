package com.ieltsaitutor.learning.notebook;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ieltsaitutor.learning.intelligence.Skill;

public record ErrorNotebookEntry(UUID id, Skill skill, String practiceSetId, String questionId, String learnerAnswer,
        String correctAnswer, String mistakeType, String evidence, int repeatCount, Instant firstSeen, Instant lastSeen,
        String resolutionState, List<String> actions, boolean repeated) {
    public ErrorNotebookEntry { actions = actions == null ? List.of("RETRY", "ASK_TUTOR", "SIMILAR_PRACTICE", "ACKNOWLEDGE") : List.copyOf(actions); }
}
