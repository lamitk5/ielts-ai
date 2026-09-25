package com.ieltsaitutor.writing;

import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WritingTutorContextTest {
    @Test
    void taskTwoCarriesTrustedTaskMetadataIntoProviderNeutralCommand() {
        AiProvider provider = mock(AiProvider.class);
        when(provider.chat(any())).thenReturn(AiChatResult.answered("""
                {"overallBandEstimate":6.0,"criteria":{},"strengths":[],"issues":[],"suggestions":[]}
                """));
        WritingAssessmentService service = new WritingAssessmentService(provider, assessment -> {});

        service.assess(UUID.randomUUID(), "task-2-opinion-01",
                "This is a sufficiently long response with a clear argument and supporting details for assessment.");

        ArgumentCaptor<AiChatCommand> commands = ArgumentCaptor.forClass(AiChatCommand.class);
        verify(provider).chat(commands.capture());
        assertThat(commands.getValue().context().taskType()).isEqualTo("TASK_2");
        assertThat(commands.getValue().context().exerciseId()).isEqualTo("task-2-opinion-01");
        assertThat(commands.getValue().context().promptId()).isEqualTo("task-2-opinion-01");
    }

    @Test
    void unknownTaskCannotTriggerAnAssessmentCall() {
        AiProvider provider = mock(AiProvider.class);
        WritingAssessment result = new WritingAssessmentService(provider, assessment -> {})
                .assess(UUID.randomUUID(), "unknown-task", "This response has enough words but no trusted task reference.");

        assertThat(result.status()).isEqualTo("UNAVAILABLE");
        verifyNoInteractions(provider);
    }
}
