package com.ieltsaitutor.rag.chat;

import com.ieltsaitutor.ai.model.AiChatCommand;

public interface RagChatService {
    RagChatResult chat(AiChatCommand command);
}
