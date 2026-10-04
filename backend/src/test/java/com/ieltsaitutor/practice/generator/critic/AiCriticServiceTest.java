package com.ieltsaitutor.practice.generator.critic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.routing.AiProviderRouter;
import com.ieltsaitutor.practice.generator.ai.RawPassagePayload;
import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;
import com.ieltsaitutor.practice.generator.validator.ValidationReport;

class AiCriticServiceTest {

    private AiProviderRouter router;
    private AiCriticService criticService;

    @BeforeEach
    void setUp() {
        router = mock(AiProviderRouter.class);
        criticService = new AiCriticService(router);
    }

    @Test
    void evaluatesPracticePackageAndReturnsAdvisoryReport() {
        when(router.chat(any(AiChatCommand.class)))
                .thenReturn(AiChatResult.answered("Pedagogical flow and item balance look excellent."));

        RawPracticePackage pkg = new RawPracticePackage(
                "Title", new RawPassagePayload("Title", List.of()), List.of(), "model", "v1", Instant.now());

        ValidationReport report = criticService.evaluate(pkg);
        assertNotNull(report);
        assertEquals("AiCritic", report.validatorName());
        assertEquals(ValidationStatus.PASS, report.status());
        assertEquals(1, report.findings().size());
        assertEquals("INFO_AI_CRITIC_ADVISORY", report.findings().get(0).code());
    }
}
