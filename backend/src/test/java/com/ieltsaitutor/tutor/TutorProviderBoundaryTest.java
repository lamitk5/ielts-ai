package com.ieltsaitutor.tutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.rag.chat.RagChatService;
import com.ieltsaitutor.tutor.context.TutorContextService;
import com.ieltsaitutor.tutor.context.TutorLearningContext;
import com.ieltsaitutor.tutor.intent.DefaultTutorIntentRouter;
import com.ieltsaitutor.tutor.tool.DeterministicTutorTools;

class TutorProviderBoundaryTest {
    @Test
    void outOfScopeRequestReturnsApprovedRedirectWithoutExternalCalls() {
        AiProvider provider = mock(AiProvider.class);
        RagChatService rag = mock(RagChatService.class);
        TutorContextService contexts = mock(TutorContextService.class);
        when(contexts.resolve(any(), any())).thenReturn(TutorLearningContext.absent("general"));
        TutorOrchestrator orchestrator = new TutorOrchestrator(provider, rag, contexts,
                new DefaultTutorIntentRouter(), mock(DeterministicTutorTools.class));

        var result = orchestrator.handle(null, request("Write JavaScript code for a banking app"));

        assertThat(result.status()).isEqualTo("OUT_OF_SCOPE");
        assertThat(result.answer()).contains("tiếng Anh và IELTS");
        verifyNoInteractions(provider, rag);
    }

    private AiChatRequest request(String message) {
        return new AiChatRequest(message, new AiChatContext("GENERAL", null, null, null, null, null, null), List.of());
    }
}
