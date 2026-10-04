package com.ieltsaitutor.tutor.tool;

import com.ieltsaitutor.tutor.context.TutorLearningContext;
import com.ieltsaitutor.tutor.intent.TutorIntent;

public class SelectedAnswerTool implements TutorTool {
    @Override public boolean supports(TutorIntent intent) { return intent == TutorIntent.APP_DATA; }
    @Override public TutorToolResult execute(TutorLearningContext context) {
        if (context == null || context.selectedAnswer() == null) return TutorToolResult.missing();
        return TutorToolResult.appData("Đáp án bạn đã chọn là " + context.selectedAnswer() + ".");
    }
}
