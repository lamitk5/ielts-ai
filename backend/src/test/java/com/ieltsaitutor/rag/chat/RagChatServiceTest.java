package com.ieltsaitutor.rag.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.AiGrounding;
import com.ieltsaitutor.ai.dto.AiSource;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.rag.retrieval.RagQuery;
import com.ieltsaitutor.rag.retrieval.RetrievedChunk;
import com.ieltsaitutor.rag.retrieval.VectorRetrievalService;

class RagChatServiceTest {
    private final AiProvider provider = mock(AiProvider.class);
    private final VectorRetrievalService retrieval = mock(VectorRetrievalService.class);
    private final RagContextBuilder contextBuilder = mock(RagContextBuilder.class);
    private final GroundingValidator validator = mock(GroundingValidator.class);
    private RagChatService service;

    @BeforeEach
    void setUp() { service = new DefaultRagChatService(provider, retrieval, contextBuilder, validator); }

    @Test
    void routesHelloDirectly() {
        when(provider.chat(any())).thenReturn(AiChatResult.answered("Hello"));

        RagChatResult result = service.chat(command("Hello", "GENERAL"));

        assertThat(result.grounding()).isEqualTo(new AiGrounding("NOT_ENABLED", false));
        verify(retrieval, never()).search(any());
    }

    @Test
    void routesRubricQuestionThroughRetrieval() {
        RetrievedChunk chunk = chunk("source-1");
        when(retrieval.search(any())).thenReturn(List.of(chunk));
        when(contextBuilder.build(List.of(chunk))).thenReturn(new RagPromptContext("evidence", List.of(new AiSource("source-1", "Rubric", "Task Response"))));
        when(validator.validate(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(provider.chat(any())).thenReturn(AiChatResult.answered("Grounded"));

        RagChatResult result = service.chat(command("Explain the writing rubric", "WRITING"));

        assertThat(result.answer()).isEqualTo("Grounded");
        assertThat(result.grounding()).isEqualTo(new AiGrounding("GROUNDED", true));
        assertThat(result.sources()).hasSize(1);
        verify(retrieval).search(any(RagQuery.class));
    }

    @Test
    void returnsInsufficientEvidenceBelowThreshold() {
        when(retrieval.search(any())).thenReturn(List.of());

        RagChatResult result = service.chat(command("Explain the writing rubric", "WRITING"));

        assertThat(result.status()).isEqualTo("INSUFFICIENT_CONTEXT");
        assertThat(result.grounding()).isEqualTo(new AiGrounding("INSUFFICIENT_EVIDENCE", true));
        assertThat(result.sources()).isEmpty();
        verify(provider, never()).chat(any());
    }

    @Test
    void preservesOnlyRetrievedSources() {
        RetrievedChunk chunk = chunk("source-1");
        when(retrieval.search(any())).thenReturn(List.of(chunk));
        when(contextBuilder.build(any())).thenReturn(new RagPromptContext("evidence", List.of(
                new AiSource("source-1", "Real", "Section"), new AiSource("invented", "Fake", "Section"))));
        when(validator.validate(any(), any())).thenReturn(List.of(new AiSource("source-1", "Real", "Section")));
        when(provider.chat(any())).thenReturn(AiChatResult.answered("Answer"));

        assertThat(service.chat(command("Explain the writing rubric", "WRITING")).sources())
                .extracting(AiSource::sourceId).containsExactly("source-1");
    }

    @Test
    void doesNotBreakGenericChatWhenRagUnavailable() {
        when(provider.chat(any())).thenReturn(AiChatResult.answered("Generic answer"));

        assertThat(service.chat(command("How are you?", "GENERAL")).answer()).isEqualTo("Generic answer");
        verify(retrieval, never()).search(any());
    }

    @Test
    void keepsExistingHistoryAndRequestId() {
        when(provider.chat(any())).thenReturn(AiChatResult.answered("Answer"));
        AiChatCommand command = new AiChatCommand("Hello", new AiChatContext("GENERAL", null, null, null, null, null, null),
                List.of(), "request-123");

        service.chat(command);

        var captor = org.mockito.ArgumentCaptor.forClass(AiChatCommand.class);
        verify(provider).chat(captor.capture());
        assertThat(captor.getValue().requestId()).isEqualTo("request-123");
        assertThat(captor.getValue().history()).isEmpty();
    }

    private AiChatCommand command(String message, String skill) {
        return new AiChatCommand(message, new AiChatContext(skill, null, null, null, null, null, null), List.of(), "request-id");
    }

    private RetrievedChunk chunk(String sourceId) {
        return new RetrievedChunk(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), sourceId, "Title", "1", 1,
                "Section", "content", .9);
    }
}
