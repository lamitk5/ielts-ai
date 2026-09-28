package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.rag.embedding.RagEmbeddingException;

class TutorAttachmentContextBuilderTest {
    private final TutorAttachmentRetrievalService retrieval = mock(TutorAttachmentRetrievalService.class);
    private final TutorAttachmentSummaryService summary = mock(TutorAttachmentSummaryService.class);
    private final TutorAttachmentContextBuilder builder = new TutorAttachmentContextBuilder(retrieval, summary,
            new TutorAttachmentContextBudget(12_000));

    @Test
    void focusedQuestionUsesTopK() {
        AttachmentChatScope scope = scope(TutorAttachmentQuestionMode.FOCUSED, List.of(UUID.randomUUID()));
        RetrievedAttachmentChunk chunk = chunk(scope.attachmentIds().get(0), 0, "focused evidence");
        when(retrieval.retrieve(scope.userId(), scope.conversationId(), scope.attachmentIds(), "Where?"))
                .thenReturn(List.of(chunk));

        AttachmentContext context = builder.build(scope, "Where?");

        verify(retrieval).retrieve(scope.userId(), scope.conversationId(), scope.attachmentIds(), "Where?");
        assertThat(context.evidence()).contains("focused evidence");
        assertThat(context.sources()).containsExactly(chunk);
    }

    @Test
    void wholeDocumentUsesHierarchicalBatches() {
        UUID id = UUID.randomUUID();
        AttachmentChatScope scope = scope(TutorAttachmentQuestionMode.WHOLE_DOCUMENT, List.of(id));
        AttachmentRepresentation representation = new AttachmentRepresentation(id, "guide.txt",
                List.of("Part 1: beginning", "Part 2: ending"), 8);
        when(summary.summarizeWholeDocument(id)).thenReturn(representation);

        AttachmentContext context = builder.build(scope, "summarize");

        assertThat(context.representations()).containsExactly(representation);
        assertThat(context.evidence()).contains("beginning", "ending");
    }

    @Test
    void comparisonRepresentsEveryFile() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        AttachmentChatScope scope = scope(TutorAttachmentQuestionMode.COMPARE, List.of(first, second));
        AttachmentRepresentation firstRepresentation = new AttachmentRepresentation(first, "one.txt", List.of("one"), 1);
        AttachmentRepresentation secondRepresentation = new AttachmentRepresentation(second, "two.txt", List.of("two"), 1);
        when(summary.compareDocuments(scope.attachmentIds())).thenReturn(List.of(firstRepresentation, secondRepresentation));

        AttachmentContext context = builder.build(scope, "compare");

        assertThat(context.representations()).containsExactly(firstRepresentation, secondRepresentation);
        assertThat(context.evidence()).contains("one", "two");
    }

    @Test
    void preservesAttachmentProvenance() {
        AttachmentChatScope scope = scope(TutorAttachmentQuestionMode.FOCUSED, List.of(UUID.randomUUID()));
        RetrievedAttachmentChunk chunk = chunk(scope.attachmentIds().get(0), 3, "source text");
        when(retrieval.retrieve(scope.userId(), scope.conversationId(), scope.attachmentIds(), "question"))
                .thenReturn(List.of(chunk));

        AttachmentContext context = builder.build(scope, "question");

        assertThat(context.evidence()).contains(chunk.filename(), "chunk 3", "source text");
    }

    @Test
    void fallsBackToAuthorizedAttachmentTextWhenVectorRetrievalIsUnavailable() {
        UUID id = UUID.randomUUID();
        AttachmentChatScope scope = scope(TutorAttachmentQuestionMode.FOCUSED, List.of(id));
        when(retrieval.retrieve(scope.userId(), scope.conversationId(), scope.attachmentIds(), "Which code?"))
                .thenThrow(new RagEmbeddingException("RAG_EMBEDDING_UNAVAILABLE", 503, "embedding unavailable"));
        when(summary.summarizeWholeDocument(id)).thenReturn(new AttachmentRepresentation(id, "notes.txt",
                List.of("SECRET_CODE=LUMEN-TXT-92841"), 1));

        AttachmentContext context = builder.build(scope, "Which code?");

        assertThat(context.evidence()).contains("LUMEN-TXT-92841");
        assertThat(context.representations()).hasSize(1);
    }

    @Test
    void neverSilentlyCutsTrailingText() {
        AttachmentChatScope scope = scope(TutorAttachmentQuestionMode.COMPARE, List.of(UUID.randomUUID()));
        String oversized = "x".repeat(48_100);
        when(summary.compareDocuments(scope.attachmentIds())).thenReturn(List.of(
                new AttachmentRepresentation(scope.attachmentIds().get(0), "large.txt", List.of(oversized), 12_100)));

        assertThatThrownBy(() -> builder.build(scope, "compare"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ATTACHMENT_CONTEXT_BUDGET_EXCEEDED");
    }

    private AttachmentChatScope scope(TutorAttachmentQuestionMode mode, List<UUID> ids) {
        return new AttachmentChatScope(UUID.randomUUID(), UUID.randomUUID(), ids, mode);
    }

    private RetrievedAttachmentChunk chunk(UUID id, int index, String content) {
        return new RetrievedAttachmentChunk(id, "source.txt", 2, "Reading", index, content, .9, null);
    }
}
