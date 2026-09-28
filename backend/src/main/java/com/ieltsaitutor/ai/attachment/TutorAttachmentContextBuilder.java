package com.ieltsaitutor.ai.attachment;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TutorAttachmentContextBuilder {
    private final TutorAttachmentRetrievalService retrieval;
    private final TutorAttachmentSummaryService summaries;
    private final TutorAttachmentContextBudget budget;

    @Autowired
    public TutorAttachmentContextBuilder(TutorAttachmentRetrievalService retrieval,
            TutorAttachmentSummaryService summaries,
            @Value("${ai.tutor.attachment.context.max-tokens:12000}") int maxAttachmentTokens) {
        this(retrieval, summaries, new TutorAttachmentContextBudget(maxAttachmentTokens));
    }

    public TutorAttachmentContextBuilder(TutorAttachmentRetrievalService retrieval,
            TutorAttachmentSummaryService summaries, TutorAttachmentContextBudget budget) {
        this.retrieval = retrieval;
        this.summaries = summaries;
        this.budget = budget;
    }

    public AttachmentContext build(AttachmentChatScope scope, String learnerQuestion) {
        if (scope == null || learnerQuestion == null || learnerQuestion.isBlank()) {
            throw new IllegalArgumentException("Attachment question is required");
        }
        if (scope.mode() == TutorAttachmentQuestionMode.FOCUSED) {
            List<RetrievedAttachmentChunk> sources = retrieval.retrieve(scope.userId(), scope.conversationId(),
                    scope.attachmentIds(), learnerQuestion);
            String evidence = formatSources(sources);
            return checked(new AttachmentContext(scope.mode(), evidence, sources, List.of(), estimateTokens(evidence)));
        }
        List<AttachmentRepresentation> representations = scope.mode() == TutorAttachmentQuestionMode.COMPARE
                ? summaries.compareDocuments(scope.attachmentIds())
                : scope.attachmentIds().stream().map(summaries::summarizeWholeDocument).toList();
        String evidence = formatRepresentations(representations);
        return checked(new AttachmentContext(scope.mode(), evidence, List.of(), representations, estimateTokens(evidence)));
    }

    private AttachmentContext checked(AttachmentContext context) {
        if (context.tokenEstimate() > budget.maxAttachmentTokens()) {
            throw new IllegalStateException("ATTACHMENT_CONTEXT_BUDGET_EXCEEDED");
        }
        return context;
    }

    private String formatSources(List<RetrievedAttachmentChunk> sources) {
        return sources.stream().map(source -> "[" + source.filename() + ", chunk " + source.chunkIndex() + "]\n"
                + source.content()).collect(java.util.stream.Collectors.joining("\n\n"));
    }

    private String formatRepresentations(List<AttachmentRepresentation> representations) {
        return representations.stream().map(representation -> "[" + representation.filename() + "]\n"
                + representation.text()).collect(java.util.stream.Collectors.joining("\n\n"));
    }

    private int estimateTokens(String text) {
        return text == null ? 0 : Math.max(1, (int) Math.ceil(text.codePointCount(0, text.length()) / 4.0));
    }
}
