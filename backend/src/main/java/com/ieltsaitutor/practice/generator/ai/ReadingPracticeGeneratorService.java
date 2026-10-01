package com.ieltsaitutor.practice.generator.ai;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.routing.AiProviderRouter;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;

@Service
public class ReadingPracticeGeneratorService {

    private final AiProviderRouter aiRouter;
    private final ReadingPromptBuilder promptBuilder;
    private final PracticeSynthesisParser parser;

    public ReadingPracticeGeneratorService(
            AiProviderRouter aiRouter,
            ReadingPromptBuilder promptBuilder,
            PracticeSynthesisParser parser) {
        this.aiRouter = aiRouter;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
    }

    public RawPracticePackage generate(BlueprintSchema blueprint, String domainTopic) {
        String prompt = promptBuilder.buildPrompt(blueprint, domainTopic);
        AiChatContext context = new AiChatContext(
                "READING",
                "practice-generation",
                "ADMIN_GENERATOR",
                "reading",
                "practice-generator",
                "ADMIN_SYSTEM",
                null
        );

        AiChatCommand command = new AiChatCommand(
                prompt,
                context,
                List.of(),
                UUID.randomUUID().toString(),
                null
        );

        AiChatResult result = aiRouter.chat(command);
        return parser.parse(result.answer(), "router", ReadingPromptBuilder.TEMPLATE_VERSION);
    }
}
