package com.ieltsaitutor.tutor.tool;

import com.ieltsaitutor.tutor.context.TutorLearningContext;
import com.ieltsaitutor.tutor.intent.TutorIntent;
import org.springframework.stereotype.Service;

/** Deterministic application-data answers. This class has no AI/provider dependency by design. */
@Service
public class DeterministicTutorTools {
    public TutorToolResult execute(TutorIntent intent, TutorLearningContext context) {
        if (intent == null || context == null) return TutorToolResult.missing();
        if (intent == TutorIntent.APP_DATA) {
            if (context.selectedAnswer() != null) return new SelectedAnswerTool().execute(context);
            if (context.questionId() != null) return new CurrentExerciseTool().execute(context);
            if (context.score() != null) return new ProgressTool().execute(context);
            if (context.correctAnswer() != null) return new CorrectAnswerTool().execute(context);
        }
        if (intent == TutorIntent.EXERCISE_EXPLANATION && context.correctAnswer() != null) {
            return new CorrectAnswerTool().execute(context);
        }
        if (intent == TutorIntent.PROGRESS_HISTORY) {
            return context.score() != null ? new ProgressTool().execute(context) : new HistoryTool().execute(context);
        }
        return TutorToolResult.missing();
    }
}
