package com.ieltsaitutor.tutor.tool;

import com.ieltsaitutor.tutor.context.TutorLearningContext;
import com.ieltsaitutor.tutor.intent.TutorIntent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicTutorToolsTest {
    private final DeterministicTutorTools tools = new DeterministicTutorTools();

    @Test
    void answersCurrentQuestionFromTrustedContext() {
        TutorToolResult result = tools.execute(TutorIntent.APP_DATA, context("reading-q1", "What is the purpose?", null, null, null));

        assertThat(result.status()).isEqualTo("APP_DATA");
        assertThat(result.answer()).contains("reading-q1", "What is the purpose?");
    }

    @Test
    void returnsSelectedAnswerAndScoreWithoutCallingAnAiDependency() {
        TutorLearningContext context = new TutorLearningContext(true, "reading", "summary", "q1", "Question",
                "C", "B", "Because", 3, 4, null, null, null, null, List.of(), null);

        TutorToolResult answer = tools.execute(TutorIntent.APP_DATA, context);
        assertThat(answer.answer()).contains("C");

        TutorLearningContext progress = new TutorLearningContext(true, "reading", "summary", null, null, null,
                null, null, 3, 4, null, null, null, null, List.of(), null);
        assertThat(tools.execute(TutorIntent.PROGRESS_HISTORY, progress).answer()).contains("3/4");
    }

    @Test
    void missingContextReturnsSafeUnavailableResult() {
        TutorToolResult result = tools.execute(TutorIntent.APP_DATA, TutorLearningContext.absent("reading"));

        assertThat(result.status()).isEqualTo("CONTEXT_MISSING");
        assertThat(result.answer()).doesNotContain("Band");
    }

    private TutorLearningContext context(String id, String prompt, String selected, String correct, String explanation) {
        return new TutorLearningContext(true, "reading", prompt, id, prompt, selected, correct, explanation,
                null, null, null, null, null, null, List.of(), null);
    }
}
