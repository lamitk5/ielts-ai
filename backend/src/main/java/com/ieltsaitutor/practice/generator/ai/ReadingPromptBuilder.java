package com.ieltsaitutor.practice.generator.ai;

import org.springframework.stereotype.Component;

import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.blueprint.ItemDistributionSpec;

@Component
public class ReadingPromptBuilder {

    public static final String TEMPLATE_VERSION = "v1.0-academic-reading";

    public String buildPrompt(BlueprintSchema blueprint, String domainTopic) {
        StringBuilder itemsDesc = new StringBuilder();
        int totalQuestions = 0;
        for (ItemDistributionSpec dist : blueprint.itemDistribution()) {
            itemsDesc.append(String.format("- Task Type: %s, Count: %d, Focus: %s\n",
                    dist.taskType(), dist.count(), dist.cognitiveSkill()));
            totalQuestions += dist.count();
        }

        String topic = (domainTopic != null && !domainTopic.isBlank())
                ? domainTopic
                : blueprint.topicCategory();

        return String.format("""
                You are an expert IELTS Academic Reading test developer.
                Generate a completely novel, academically rigorous IELTS Reading Practice Set based on the following Blueprint.

                TARGET SPECIFICATIONS:
                - Target IELTS Band: %.1f
                - Topic Domain: %s
                - Target Passage Word Count: %d words (between 600 and 900 words)
                - Paragraph Count: %d numbered paragraphs (p1, p2, p3, ...)
                - Rhetorical Pattern: %s
                - Lexical & Syntactic Density: %s

                QUESTION ITEM SPECIFICATIONS (Total: %d items):
                %s

                MANDATORY RULES:
                1. The passage MUST be 100%% original. Do not copy copyrighted IELTS or external texts.
                2. Every question MUST be answerable ONLY from the explicit text in the generated passage.
                3. For every question, you MUST provide:
                   - "evidenceSpan": an EXACT, VERBATIM substring from the generated passage that proves the answer.
                   - "answerKey": the single unambiguous correct answer (e.g., "TRUE", "FALSE", "NOT GIVEN", "A", "B", "C", "D", or exact 1-3 words).
                   - "explanation": a clear rationale referencing the evidence span.
                4. For Multiple Choice questions, provide 4 options ("A", "B", "C", "D") with distinct, plausible distractors.

                OUTPUT FORMAT:
                You MUST output ONLY valid JSON matching this schema with no extra surrounding text:
                {
                  "title": "Passage Title",
                  "passage": {
                    "title": "Passage Title",
                    "paragraphs": [
                      { "id": "p1", "text": "Paragraph 1 text..." },
                      { "id": "p2", "text": "Paragraph 2 text..." }
                    ]
                  },
                  "questions": [
                    {
                      "id": "q1",
                      "taskType": "TRUE_FALSE_NOT_GIVEN",
                      "prompt": "Question prompt text...",
                      "options": ["TRUE", "FALSE", "NOT GIVEN"],
                      "answerKey": "TRUE",
                      "evidenceSpan": "Exact verbatim substring from paragraph 1 text",
                      "explanation": "Explanation text..."
                    }
                  ]
                }
                """,
                blueprint.targetBand().doubleValue(),
                topic,
                blueprint.passageStructure().targetWordCount(),
                blueprint.passageStructure().paragraphCount(),
                blueprint.passageStructure().rhetoricalPattern(),
                blueprint.passageStructure().lexicalDensity(),
                totalQuestions,
                itemsDesc.toString()
        );
    }
}
