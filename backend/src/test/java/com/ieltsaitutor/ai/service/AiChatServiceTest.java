package com.ieltsaitutor.ai.service;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.ai.dto.AiChatResponse;
import com.ieltsaitutor.ai.dto.ChatHistoryItem;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatServiceTest {
    private final AiProvider provider = mock(AiProvider.class);
    private final AiChatService service = new AiChatService(provider);

    @Test
    void delegatesTrimmedMessageAndBoundedHistoryToProvider() {
        when(provider.chat(any())).thenReturn(AiChatResult.answered("Answer"));
        var history = java.util.stream.IntStream.range(0, 12)
                .mapToObj(index -> new ChatHistoryItem("USER", "message-" + index)).toList();

        AiChatResponse response = service.chat(new AiChatRequest(
                "  Hello IELTS  ",
                new AiChatContext("READING", null, null, null, null, null, null), history));

        assertThat(response.status()).isEqualTo("ANSWERED");
        assertThat(response.answer()).isEqualTo("Answer");
        var command = org.mockito.ArgumentCaptor.forClass(AiChatCommand.class);
        verify(provider).chat(command.capture());
        assertThat(command.getValue().message()).isEqualTo("Hello IELTS");
        assertThat(command.getValue().requestId()).isEqualTo(response.meta().requestId());
        assertThat(command.getValue().history()).hasSize(8);
        assertThat(command.getValue().history().getFirst().content()).isEqualTo("message-4");
    }

    @Test
    void preservesInsufficientContextResultWithoutCallingItAnswered() {
        when(provider.chat(any())).thenReturn(AiChatResult.insufficientContext(
                "Chưa đủ thông tin để trả lời chắc chắn. Hãy cung cấp câu hỏi, đoạn văn hoặc bài làm liên quan."));

        AiChatResponse response = service.chat(new AiChatRequest(
                "Tại sao câu 14 là FALSE?",
                new AiChatContext("READING", null, null, null, null, null, null), List.of()));

        assertThat(response.status()).isEqualTo("INSUFFICIENT_CONTEXT");
        assertThat(response.answer()).startsWith("Chưa đủ thông tin");
        assertThat(response.sources()).isEmpty();
    }
}
