package com.ieltsaitutor.tutor.intent;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.tutor.context.TutorLearningContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TutorIntentRouterTest {
    private final TutorIntentRouter router = new DefaultTutorIntentRouter();

    @Test void routesVietnameseAppDataBeforeAi() {
        TutorIntentRoute route = router.route(request("Tôi đang làm câu nào?", "READING"), context(true, "q1"));
        assertThat(route.intent()).isEqualTo(TutorIntent.APP_DATA);
        assertThat(route.externalAiAllowed()).isFalse();
    }

    @Test void routesEnglishAndVietnameseProgressHistory() {
        assertThat(router.route(request("What was my score?", "READING"), context(true, null)).intent())
                .isEqualTo(TutorIntent.APP_DATA);
        assertThat(router.route(request("xem tiến bộ của tôi", "GENERAL"), context(true, null)).intent())
                .isEqualTo(TutorIntent.PROGRESS_HISTORY);
    }

    @Test void routesExerciseWritingSpeakingAndRag() {
        assertThat(router.route(request("Why is my answer wrong?", "READING"), context(true, "q1")).intent())
                .isEqualTo(TutorIntent.EXERCISE_EXPLANATION);
        assertThat(router.route(request("Please review my writing draft", "WRITING"), context(true, null)).intent())
                .isEqualTo(TutorIntent.WRITING_FEEDBACK);
        assertThat(router.route(request("Give feedback on my speaking", "SPEAKING"), context(true, null)).intent())
                .isEqualTo(TutorIntent.SPEAKING_FEEDBACK);
        assertThat(router.route(request("Explain the IELTS rubric with sources", "GENERAL"), context(true, null)).intent())
                .isEqualTo(TutorIntent.RAG_EXPLANATION);
    }

    @Test void genericChatDoesNotEnableRagAndMissingExerciseContextCannotFabricate() {
        TutorIntentRoute generic = router.route(request("hello", "GENERAL"), context(false, null));
        TutorIntentRoute missing = router.route(request("Why is this answer wrong?", "READING"), context(false, null));

        assertThat(generic.intent()).isEqualTo(TutorIntent.GENERIC_CHAT);
        assertThat(generic.ragAllowed()).isFalse();
        assertThat(missing.intent()).isEqualTo(TutorIntent.EXERCISE_EXPLANATION);
        assertThat(missing.externalAiAllowed()).isFalse();
    }

    private AiChatRequest request(String message, String skill) {
        return new AiChatRequest(message, new AiChatContext(skill, null, null, null, null, null, null), List.of());
    }

    private TutorLearningContext context(boolean available, String question) {
        return new TutorLearningContext(available, "reading", "", question, question, null, null, null,
                null, null, null, null, null, null, List.of(), null);
    }
}
