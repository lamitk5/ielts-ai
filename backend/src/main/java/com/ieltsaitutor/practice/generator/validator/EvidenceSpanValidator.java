package com.ieltsaitutor.practice.generator.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ieltsaitutor.practice.generator.ai.RawPassagePayload;
import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.ai.RawQuestionPayload;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;

@Component
public class EvidenceSpanValidator implements PracticeValidator {

    public static final String POLICY_VERSION = "evidence-span-policy-v1.0";

    @Override
    public String name() {
        return "EvidenceSpanValidator";
    }

    @Override
    public ValidationReport validate(RawPracticePackage pkg, BlueprintSchema blueprint, PracticeGenerationSource source) {
        List<ValidationFinding> findings = new ArrayList<>();

        String fullPassageText = normalize(pkg.passage().paragraphs().stream()
                .map(RawPassagePayload.RawParagraphPayload::text)
                .collect(Collectors.joining(" ")));

        for (RawQuestionPayload q : pkg.questions()) {
            String span = q.evidenceSpan() != null ? q.evidenceSpan().trim() : "";
            String key = q.answerKey() != null ? q.answerKey().trim().toUpperCase() : "";

            // For NOT GIVEN items, evidence span may be optional or topical anchor
            if ("NOT GIVEN".equals(key)) {
                if (span.isBlank()) {
                    findings.add(new ValidationFinding("INFO_NOT_GIVEN_SPAN_EMPTY", ValidationStatus.PASS, q.id(),
                            "NOT GIVEN item has no evidence span (concept absent from text)", ""));
                }
                continue;
            }

            if (span.isBlank()) {
                findings.add(new ValidationFinding("ERR_MISSING_EVIDENCE_SPAN", ValidationStatus.FAIL, q.id(),
                        "Question requires an evidence span in the passage proving the answer key", ""));
                continue;
            }

            String normalizedSpan = normalize(span);
            if (!fullPassageText.contains(normalizedSpan)) {
                findings.add(new ValidationFinding("ERR_EVIDENCE_SPAN_NOT_VERBATIM", ValidationStatus.FAIL, q.id(),
                        String.format("Evidence span '%s' was not found verbatim in the generated passage text", span),
                        ""));
            }
        }

        boolean hasFail = findings.stream().anyMatch(f -> f.severity() == ValidationStatus.FAIL);
        boolean hasWarn = findings.stream().anyMatch(f -> f.severity() == ValidationStatus.WARNING);
        ValidationStatus overall = hasFail ? ValidationStatus.FAIL : (hasWarn ? ValidationStatus.WARNING : ValidationStatus.PASS);

        return new ValidationReport(name(), POLICY_VERSION, overall, findings);
    }

    private String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase().replaceAll("\\s+", " ").trim();
    }
}
