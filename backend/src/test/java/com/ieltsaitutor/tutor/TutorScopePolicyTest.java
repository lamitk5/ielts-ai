package com.ieltsaitutor.tutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.tutor.context.TutorLearningContext;
import com.ieltsaitutor.tutor.intent.DefaultTutorIntentRouter;
import com.ieltsaitutor.tutor.intent.TutorIntent;

class TutorScopePolicyTest {
    private final DefaultTutorIntentRouter router = new DefaultTutorIntentRouter();

    @Test
    void unrelatedAssistantRequestsAreProviderFreeOutOfScope() {
        var route = router.route(request("Write JavaScript code for a banking app"), TutorLearningContext.absent("general"));
        assertThat(route.intent()).isEqualTo(TutorIntent.OUT_OF_SCOPE);
        assertThat(route.externalAiAllowed()).isFalse();
        assertThat(route.ragAllowed()).isFalse();
    }

    @Test
    void generalTopicExplicitlyFramedAsEnglishPracticeRemainsInScope() {
        var route = router.route(request("Talk with me about football in English"), TutorLearningContext.absent("general"));
        assertThat(route.intent()).isEqualTo(TutorIntent.GENERIC_CHAT);
        assertThat(route.externalAiAllowed()).isTrue();
    }

    private AiChatRequest request(String message) {
        return new AiChatRequest(message, new AiChatContext("GENERAL", null, null, null, null, null, null), List.of());
    }
}
