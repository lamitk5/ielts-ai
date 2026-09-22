package com.ieltsaitutor.ai.provider;

import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;

public interface AiProvider {
    AiChatResult chat(AiChatCommand command);
}
