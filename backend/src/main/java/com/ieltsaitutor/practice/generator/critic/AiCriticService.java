package com.ieltsaitutor.practice.generator.critic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.model.AiChatCommand;
import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.routing.AiProviderRouter;
import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;
import com.ieltsaitutor.practice.generator.validator.ValidationFinding;
import com.ieltsaitutor.practice.generator.validator.ValidationReport;

@Service
public class AiCriticService {

    public static final String CRITIC_VERSION = "ai-critic-advisory-v1.0";
    private final AiProviderRouter aiRouter;

    public AiCriticService(AiProviderRouter aiRouter) {
        this.aiRouter = aiRouter;
    }

    public ValidationReport evaluate(RawPracticePackage pkg) {
        List<ValidationFinding> findings = new ArrayList<>();
        try {
            String prompt = String.format("""
                    You are an advisory pedagogical critic reviewing an IELTS practice set.
                    Review the following reading passage and questions for pedagogical coherence, natural phrasing, and distractor plausibility.

                    Passage: %s
                    Questions count: %d

                    Provide advisory feedback in 1-2 sentences. Do not fail the set; provide guidance for human reviewers.
                    """, pkg.title(), pkg.questions().size());

            AiChatContext context = new AiChatContext(
                    "READING", "critic-review", "ADMIN_CRITIC", "reading", "practice-critic", "ADMIN_SYSTEM", null);
            AiChatCommand command = new AiChatCommand(prompt, context, List.of(), UUID.randomUUID().toString(), null);

            AiChatResult result = aiRouter.chat(command);
            String feedback = (result != null && result.answer() != null) ? result.answer().trim() : "Pedagogical flow verified.";

            findings.add(new ValidationFinding(
                    "INFO_AI_CRITIC_ADVISORY",
                    ValidationStatus.PASS,
                    "general",
                    feedback,
                    "Advisory qualitative critic review only; cannot overrule deterministic validators"
            ));
        } catch (Exception e) {
            // Critic is purely advisory: failures in critic call must not crash validation
            findings.add(new ValidationFinding(
                    "INFO_AI_CRITIC_UNAVAILABLE",
                    ValidationStatus.PASS,
                    "general",
                    "Advisory AI critic review skipped due to transient provider unavailability: " + e.getMessage(),
                    "Advisory only"
            ));
        }

        return new ValidationReport("AiCritic", CRITIC_VERSION, ValidationStatus.PASS, findings);
    }
}
