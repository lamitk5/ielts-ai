package com.ieltsaitutor.tutor.tool;

import com.ieltsaitutor.tutor.context.TutorLearningContext;
import com.ieltsaitutor.tutor.intent.TutorIntent;

public class HistoryTool implements TutorTool {
    @Override public boolean supports(TutorIntent intent) { return intent == TutorIntent.PROGRESS_HISTORY; }
    @Override public TutorToolResult execute(TutorLearningContext context) {
        if (context == null || !context.available() || context.contextText().isBlank()) return TutorToolResult.missing();
        return TutorToolResult.appData("Lịch sử học tập gần đây: " + context.contextText());
    }
}
