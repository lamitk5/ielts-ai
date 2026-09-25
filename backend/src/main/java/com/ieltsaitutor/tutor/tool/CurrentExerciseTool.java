package com.ieltsaitutor.tutor.tool;

import com.ieltsaitutor.tutor.context.TutorLearningContext;
import com.ieltsaitutor.tutor.intent.TutorIntent;

public class CurrentExerciseTool implements TutorTool {
    @Override public boolean supports(TutorIntent intent) { return intent == TutorIntent.APP_DATA || intent == TutorIntent.EXERCISE_EXPLANATION; }
    @Override public TutorToolResult execute(TutorLearningContext context) {
        if (context == null || !context.available() || context.questionId() == null) return TutorToolResult.missing();
        return TutorToolResult.appData("Bạn đang làm câu " + context.questionId() + ": " + context.questionPrompt());
    }
}
