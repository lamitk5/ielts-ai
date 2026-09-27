package com.ieltsaitutor.writing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;

class WritingProviderUnavailableTest {
    @Test
    void malformedAndUnavailableProviderPreserveSubmissionWithoutFabricatingFeedback() {
        for (AiChatResult providerResult : new AiChatResult[]{AiChatResult.answered("not-json"), new AiChatResult("AI_RATE_LIMITED", "busy")}) {
            AiProvider provider = mock(AiProvider.class);
            when(provider.chat(any())).thenReturn(providerResult);
            String response = "This is a sufficiently long essay response with a clear position and supporting examples for review.";
            WritingAssessment result = new WritingAssessmentService(provider, mock(WritingRepository.class)).assess(
                    UUID.randomUUID(), "task-2-opinion-01", response);
            assertEquals("UNAVAILABLE", result.status());
            assertNull(result.overallBandEstimate());
            assertEquals(response, result.submittedText());
        }
    }
}
