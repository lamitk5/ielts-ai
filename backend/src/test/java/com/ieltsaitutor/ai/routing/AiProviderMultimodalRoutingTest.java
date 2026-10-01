package com.ieltsaitutor.ai.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.ieltsaitutor.ai.exception.AiProviderException;
import com.ieltsaitutor.ai.model.AiAttachmentPart;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.ProviderCapability;
import com.ieltsaitutor.ai.provider.ProviderId;

class AiProviderMultimodalRoutingTest {
    @Test
    void imageTurnSkipsTextOnlyProvider() {
        AiProviderAdapter textOnly = adapter(ProviderId.GROQ, Set.of(ProviderCapability.CHAT), "text");
        AiProviderAdapter vision = adapter(ProviderId.GEMINI,
                Set.of(ProviderCapability.CHAT, ProviderCapability.VISION_IMAGE), "vision");
        AiChatCommand command = multimodal(ProviderCapability.VISION_IMAGE);

        AiProviderRouter router = new AiProviderRouter(List.of(textOnly, vision));
        clearInvocations(textOnly, vision);
        assertThat(router.chat(command).answer()).isEqualTo("vision");
        verify(textOnly, org.mockito.Mockito.never()).chat(org.mockito.ArgumentMatchers.any(AiChatCommand.class));
        verify(vision).chat(command);
    }

    @Test
    void documentContextRequiresDeclaredCapability() {
        AiProviderAdapter textOnly = adapter(ProviderId.GROQ, Set.of(ProviderCapability.CHAT), "text");
        AiProviderAdapter document = adapter(ProviderId.CLOUDFLARE,
                Set.of(ProviderCapability.CHAT, ProviderCapability.DOCUMENT_CONTEXT), "document");

        AiProviderRouter router = new AiProviderRouter(List.of(textOnly, document));
        clearInvocations(textOnly, document);
        assertThat(router.chat(multimodal(ProviderCapability.DOCUMENT_CONTEXT)).answer())
                .isEqualTo("document");
        verify(textOnly, org.mockito.Mockito.never()).chat(org.mockito.ArgumentMatchers.any(AiChatCommand.class));
    }

    @Test
    void textOnlyTurnKeepsExistingOrder() {
        AiProviderAdapter first = adapter(ProviderId.GROQ, Set.of(ProviderCapability.CHAT), "first");
        AiProviderAdapter second = adapter(ProviderId.GEMINI, Set.of(ProviderCapability.CHAT), "second");

        AiProviderRouter router = new AiProviderRouter(List.of(first, second));
        clearInvocations(first, second);
        assertThat(router.chat(new AiChatCommand("hello", null, List.of(), "r1")).answer())
                .isEqualTo("first");
        verify(first).chat(org.mockito.ArgumentMatchers.any(AiChatCommand.class));
        verify(second, org.mockito.Mockito.never()).chat(org.mockito.ArgumentMatchers.any(AiChatCommand.class));
    }

    @Test
    void noVisionProviderReturnsSafeUnavailable() {
        AiProviderAdapter textOnly = adapter(ProviderId.GROQ, Set.of(ProviderCapability.CHAT), "text");

        assertThatThrownBy(() -> new AiProviderRouter(List.of(textOnly)).chat(multimodal(ProviderCapability.VISION_IMAGE)))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("Trợ giảng AI tạm thời chưa sẵn sàng.");
    }

    @Test
    void providerFailureDoesNotOpenOrMutateAttachment() {
        @SuppressWarnings("unchecked")
        Supplier<java.io.InputStream> opener = mock(Supplier.class);
        AiProviderAdapter failing = adapter(ProviderId.GEMINI, Set.of(ProviderCapability.CHAT, ProviderCapability.VISION_IMAGE), "unused");
        when(failing.chat(org.mockito.ArgumentMatchers.any(AiChatCommand.class))).thenThrow(
                new AiProviderException("AI_TEMPORARILY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "down"));
        AiChatCommand command = new AiChatCommand("describe", null, List.of(), "r1", null,
                List.of(new AiAttachmentPart(UUID.randomUUID(), "photo.png", "image/png",
                        com.ieltsaitutor.ai.attachment.AttachmentKind.IMAGE, opener)),
                Set.of(ProviderCapability.VISION_IMAGE));

        assertThatThrownBy(() -> new AiProviderRouter(List.of(failing)).chat(command)).isInstanceOf(AiProviderException.class);
        org.mockito.Mockito.verifyNoInteractions(opener);
    }

    private AiChatCommand multimodal(ProviderCapability capability) {
        return new AiChatCommand("question", null, List.of(), "r1", null,
                List.of(new AiAttachmentPart(UUID.randomUUID(), "file.txt", "text/plain",
                        com.ieltsaitutor.ai.attachment.AttachmentKind.DOCUMENT,
                        () -> new ByteArrayInputStream(new byte[] { 1 }))), Set.of(capability));
    }

    private AiProviderAdapter adapter(ProviderId id, Set<ProviderCapability> capabilities, String answer) {
        AiProviderAdapter adapter = mock(AiProviderAdapter.class);
        when(adapter.id()).thenReturn(id);
        when(adapter.enabled()).thenReturn(true);
        when(adapter.capabilities()).thenReturn(capabilities);
        when(adapter.chat(org.mockito.ArgumentMatchers.any(AiChatCommand.class))).thenReturn(AiChatResult.answered(answer));
        return adapter;
    }
}
