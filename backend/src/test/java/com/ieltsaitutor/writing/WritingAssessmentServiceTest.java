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

class WritingAssessmentServiceTest {
    @Test
    void validProviderAssessmentKeepsEstimateDisclaimer() {
        AiProvider provider = mock(AiProvider.class);
        when(provider.chat(any())).thenReturn(AiChatResult.answered("""
                {"overallBandEstimate":6.5,"criteria":{"Task response":"6.5","Coherence":"6.0"},"strengths":["Clear position"],"issues":["Examples need detail"],"suggestions":["Add specific evidence"]}
                """));

        WritingAssessment result = new WritingAssessmentService(provider, new FakeWritingRepository()).assess(
                UUID.randomUUID(), "task-1", "This is a sufficiently long writing response with a clear position and supporting examples for review.");

        assertEquals("ANSWERED", result.status());
        assertEquals(6.5, result.overallBandEstimate());
        assertEquals("Band ước lượng — không phải điểm thi chính thức.", result.disclaimer());
    }

    @Test
    void malformedOrUnavailableProviderNeverFabricatesFeedback() {
        for (AiChatResult providerResult : new AiChatResult[]{
                AiChatResult.answered("not-json"),
                new AiChatResult("AI_PROVIDER_ERROR", "unavailable"),
                new AiChatResult("AI_RATE_LIMITED", "busy")}) {
            AiProvider provider = mock(AiProvider.class);
            when(provider.chat(any())).thenReturn(providerResult);
            WritingAssessment result = new WritingAssessmentService(provider, new FakeWritingRepository()).assess(
                    UUID.randomUUID(), "task-1", "This is a sufficiently long writing response with a clear position and supporting examples for review.");
            assertEquals("UNAVAILABLE", result.status());
            assertNull(result.overallBandEstimate());
        }
    }

    @Test
    void malformedProviderResultPersistsOneHistoryEntry() {
        AiProvider provider = mock(AiProvider.class);
        when(provider.chat(any())).thenReturn(AiChatResult.answered("not-json"));
        CountingWritingRepository repository = new CountingWritingRepository();

        WritingAssessment result = new WritingAssessmentService(provider, repository).assess(
                UUID.randomUUID(), "task-1", "This is a sufficiently long writing response with a clear position and supporting examples for review.");

        assertEquals("UNAVAILABLE", result.status());
        assertEquals(1, repository.saves);
    }

    static final class FakeWritingRepository implements WritingRepository {
        @Override public void save(WritingAssessment assessment) {}
    }

    static final class CountingWritingRepository implements WritingRepository {
        int saves;
        @Override public void save(WritingAssessment assessment) { saves++; }
    }
}
