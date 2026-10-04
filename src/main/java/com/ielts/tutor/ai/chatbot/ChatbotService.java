package com.ielts.tutor.ai.chatbot;

import com.ielts.tutor.shared.SharedComponent;
import org.springframework.stereotype.Service;

@Service
public class ChatbotService {

    private final ChatbotEvaluator chatbotEvaluator;
    private final SharedComponent sharedComponent;

    public ChatbotService(ChatbotEvaluator chatbotEvaluator, SharedComponent sharedComponent) {
        this.chatbotEvaluator = chatbotEvaluator;
        this.sharedComponent = sharedComponent;
    }
}
