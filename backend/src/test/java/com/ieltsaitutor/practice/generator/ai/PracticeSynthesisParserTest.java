package com.ieltsaitutor.practice.generator.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PracticeSynthesisParserTest {

    @Test
    void parsesValidRawJsonWithCodeFences() {
        String aiResponse = """
                Here is the generated reading set:
                ```json
                {
                  "title": "Arctic Estuarine Plastic Dynamics",
                  "passage": {
                    "title": "Arctic Estuarine Plastic Dynamics",
                    "paragraphs": [
                      { "id": "p1", "text": "In high-latitude river deltas, sediment sampling has uncovered plastic." },
                      { "id": "p2", "text": "Contrary to initial hypotheses that sea ice prevents settling, particles sink." }
                    ]
                  },
                  "questions": [
                    {
                      "id": "q1",
                      "taskType": "TRUE_FALSE_NOT_GIVEN",
                      "prompt": "Sea ice completely prevents plastic settling.",
                      "options": ["TRUE", "FALSE", "NOT GIVEN"],
                      "answerKey": "FALSE",
                      "evidenceSpan": "Contrary to initial hypotheses that sea ice prevents settling",
                      "explanation": "Paragraph 2 directly contradicts this claim."
                    }
                  ]
                }
                ```
                Hope this is helpful!
                """;

        PracticeSynthesisParser parser = new PracticeSynthesisParser();
        RawPracticePackage pkg = parser.parse(aiResponse, "gemini-1.5-pro", "v1.0");

        assertNotNull(pkg);
        assertEquals("Arctic Estuarine Plastic Dynamics", pkg.title());
        assertEquals(2, pkg.passage().paragraphs().size());
        assertEquals("p1", pkg.passage().paragraphs().get(0).id());
        assertEquals(1, pkg.questions().size());
        assertEquals("q1", pkg.questions().get(0).id());
        assertEquals("FALSE", pkg.questions().get(0).answerKey());
        assertEquals("Contrary to initial hypotheses that sea ice prevents settling", pkg.questions().get(0).evidenceSpan());
    }
}
