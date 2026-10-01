package com.ieltsaitutor.tutor.tool;

import com.ieltsaitutor.tutor.context.TutorLearningContext;
import com.ieltsaitutor.tutor.intent.TutorIntent;

public class ProgressTool implements TutorTool {
    @Override public boolean supports(TutorIntent intent) { return intent == TutorIntent.APP_DATA || intent == TutorIntent.PROGRESS_HISTORY; }
    @Override public TutorToolResult execute(TutorLearningContext context) {
        if (context == null || !context.available() || context.score() == null || context.total() == null) return TutorToolResult.missing();
        return TutorToolResult.appData("Kết quả gần nhất: " + context.score() + "/" + context.total() + ".");
    }
}
