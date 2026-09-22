package com.ieltsaitutor.ai.model;

public record AiChatResult(String status, String answer) {
    public static AiChatResult answered(String answer) {
        return new AiChatResult("ANSWERED", answer);
    }

    public static AiChatResult insufficientContext(String answer) {
        return new AiChatResult("INSUFFICIENT_CONTEXT", answer);
    }
}
