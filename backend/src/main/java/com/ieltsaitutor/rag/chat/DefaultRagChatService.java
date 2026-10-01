package com.ieltsaitutor.rag.chat;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.ai.dto.AiGrounding;
import com.ieltsaitutor.ai.dto.AiSource;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.rag.domain.Skill;
import com.ieltsaitutor.rag.retrieval.RagQuery;
import com.ieltsaitutor.rag.retrieval.RetrievedChunk;
import com.ieltsaitutor.rag.retrieval.VectorRetrievalService;

@Service
public class DefaultRagChatService implements RagChatService {
    private static final String INSUFFICIENT = "Chưa tìm thấy nguồn đủ phù hợp để trả lời chắc chắn.";
    private static final String UNAVAILABLE = "Nguồn học liệu hiện chưa sẵn sàng. Vui lòng thử lại sau.";
    private final AiProvider provider;
    private final VectorRetrievalService retrieval;
    private final RagContextBuilder contextBuilder;
    private final GroundingValidator groundingValidator;
    private final RagIntentClassifier classifier;

    @Autowired
    public DefaultRagChatService(AiProvider provider, VectorRetrievalService retrieval,
            RagContextBuilder contextBuilder, GroundingValidator groundingValidator) {
        this(provider, retrieval, contextBuilder, groundingValidator, new RagIntentClassifier());
    }

    public DefaultRagChatService(AiProvider provider, VectorRetrievalService retrieval,
            RagContextBuilder contextBuilder, GroundingValidator groundingValidator, RagIntentClassifier classifier) {
        this.provider = provider;
        this.retrieval = retrieval;
        this.contextBuilder = contextBuilder;
        this.groundingValidator = groundingValidator;
        this.classifier = classifier;
    }

    @Override
    public RagChatResult chat(AiChatCommand command) {
        if (!classifier.requiresRetrieval(command)) {
            AiChatResult direct = provider.chat(command);
            return new RagChatResult(direct.status(), direct.answer(), List.of(), new AiGrounding("NOT_ENABLED", false));
        }
        try {
            var context = command.context();
            RagQuery query = new RagQuery(command.message(), skill(context), null,
                    context == null ? null : context.lessonId(), context == null ? null : context.exerciseId(), 0, 0);
            List<RetrievedChunk> chunks = retrieval.search(query);
            if (chunks.isEmpty()) {
                return new RagChatResult("INSUFFICIENT_CONTEXT", INSUFFICIENT, List.of(),
                        new AiGrounding("INSUFFICIENT_EVIDENCE", true));
            }
            RagPromptContext prompt = contextBuilder.build(chunks);
            AiChatResult response = provider.chat(new AiChatCommand(command.message(), command.context(), command.history(),
                    command.requestId(), prompt.evidence()));
            Set<String> retrievedIds = chunks.stream().map(RetrievedChunk::sourceId).filter(id -> id != null).collect(java.util.stream.Collectors.toSet());
            List<AiSource> sources = groundingValidator.validate(prompt.sources(), retrievedIds);
            return new RagChatResult(response.status(), response.answer(), sources, new AiGrounding("GROUNDED", true));
        } catch (RuntimeException exception) {
            return new RagChatResult("RAG_UNAVAILABLE", UNAVAILABLE, List.of(), new AiGrounding("RAG_ERROR", true));
        }
    }

    private Skill skill(com.ieltsaitutor.ai.dto.AiChatContext context) {
        try { return context == null ? Skill.GENERAL : Skill.valueOf(context.normalizedSkill()); }
        catch (IllegalArgumentException invalid) { return Skill.GENERAL; }
    }
}
