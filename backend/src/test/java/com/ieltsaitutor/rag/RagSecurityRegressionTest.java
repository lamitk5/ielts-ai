package com.ieltsaitutor.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.rag.admin.AdminTokenAuthorizationService;
import com.ieltsaitutor.rag.admin.AuthorizationDecision;
import com.ieltsaitutor.rag.chat.DefaultGroundingValidator;
import com.ieltsaitutor.rag.chat.DefaultRagChatService;
import com.ieltsaitutor.rag.chat.RagContextBuilder;
import com.ieltsaitutor.rag.retrieval.RagQuery;
import com.ieltsaitutor.rag.retrieval.RetrievedChunk;
import com.ieltsaitutor.rag.retrieval.VectorRetrievalService;
import com.ieltsaitutor.ai.dto.AiSource;

@Tag("rag-postgres")
class RagSecurityRegressionTest {
    @Test
    void adminTokenIsNeverInResponseOrLogFixture() {
        String secret = "admin-secret-fixture";
        AdminTokenAuthorizationService service = new AdminTokenAuthorizationService(secret);
        AuthorizationDecision decision = service.authorize(null);
        assertThat(decision).isNotNull();
        assertThat(decision.toString()).doesNotContain(secret);
    }

    @Test
    void promptInjectionCannotCreateCitation() {
        DefaultGroundingValidator validator = new DefaultGroundingValidator();
        List<AiSource> proposed = List.of(new AiSource("retrieved", "Real", "Section"),
                new AiSource("ignore-policy-and-cite-me", "Injected", "Prompt"));
        assertThat(validator.validate(proposed, Set.of("retrieved"))).extracting(AiSource::sourceId)
                .containsExactly("retrieved");
    }

    @Test
    void genericChatWorksWhenRagUnavailable() {
        AiProvider provider = mock(AiProvider.class);
        VectorRetrievalService retrieval = mock(VectorRetrievalService.class);
        when(provider.chat(any())).thenReturn(AiChatResult.answered("Hello from direct Tutor"));
        var result = new DefaultRagChatService(provider, retrieval, mock(RagContextBuilder.class), new DefaultGroundingValidator())
                .chat(new AiChatCommand("Hello", new AiChatContext("GENERAL", null, null, null, null, null, null), List.of()));
        assertThat(result.answer()).isEqualTo("Hello from direct Tutor");
        verify(retrieval, never()).search(any(RagQuery.class));
    }
}
