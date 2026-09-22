package com.ieltsaitutor.ai.controller;

import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.ai.dto.AiChatResponse;
import com.ieltsaitutor.ai.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {
    private final AiChatService service;

    public AiChatController(AiChatService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return service.chat(request);
    }
}
