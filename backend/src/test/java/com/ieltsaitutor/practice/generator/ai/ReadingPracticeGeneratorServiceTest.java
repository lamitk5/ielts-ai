package com.ieltsaitutor.practice.generator.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.routing.AiProviderRouter;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.blueprint.ItemDistributionSpec;
import com.ieltsaitutor.practice.generator.blueprint.PassageStructureSpec;
import com.ieltsaitutor.practice.generator.blueprint.ReasoningRulesSpec;
import com.ieltsaitutor.rag.domain.Skill;

class ReadingPracticeGeneratorServiceTest {

    private AiProviderRouter aiRouter;
    private ReadingPracticeGeneratorService generatorService;

    @BeforeEach
    void setUp() {
        aiRouter = mock(AiProviderRouter.class);
        ReadingPromptBuilder promptBuilder = new ReadingPromptBuilder();
        PracticeSynthesisParser parser = new PracticeSynthesisParser();
        generatorService = new ReadingPracticeGeneratorService(aiRouter, promptBuilder, parser);
    }

    @Test
    void generatesAndParsesReadingPracticePackage() {
        BlueprintSchema blueprint = new BlueprintSchema(
                "bp-1",
                Skill.READING,
                BigDecimal.valueOf(7.0),
                "NATURAL_SCIENCES",
                new PassageStructureSpec(750, 5, "EXPOSITORY", "ACADEMIC_CEFR_C1"),
                List.of(new ItemDistributionSpec("TRUE_FALSE_NOT_GIVEN", 3, List.of(1, 2), "FACTUAL_DETAIL")),
                new ReasoningRulesSpec(true, true, 3)
        );

        String jsonPayload = """
                {
                  "title": "Ocean Acidification",
                  "passage": {
                    "title": "Ocean Acidification",
                    "paragraphs": [
                      { "id": "p1", "text": "Ocean pH levels have dropped significantly over the past century." }
                    ]
                  },
                  "questions": [
                    {
                      "id": "q1",
                      "taskType": "TRUE_FALSE_NOT_GIVEN",
                      "prompt": "Ocean pH has dropped in the last century.",
                      "options": ["TRUE", "FALSE", "NOT GIVEN"],
                      "answerKey": "TRUE",
                      "evidenceSpan": "Ocean pH levels have dropped significantly over the past century",
                      "explanation": "Paragraph 1 confirms the reduction in pH."
                    }
                  ]
                }
                """;

        when(aiRouter.chat(any(AiChatCommand.class)))
                .thenReturn(AiChatResult.answered(jsonPayload));

        RawPracticePackage pkg = generatorService.generate(blueprint, "Ocean Acidification");

        assertNotNull(pkg);
        assertEquals("Ocean Acidification", pkg.title());
        assertEquals(1, pkg.passage().paragraphs().size());
        assertEquals(1, pkg.questions().size());
    }
}
