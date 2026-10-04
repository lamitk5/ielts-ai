package com.ieltsaitutor.practice.generator.similarity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ieltsaitutor.practice.generator.ai.RawPassagePayload;
import com.ieltsaitutor.practice.generator.ai.RawPracticePackage;
import com.ieltsaitutor.practice.generator.blueprint.BlueprintSchema;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;
import com.ieltsaitutor.practice.generator.validator.PracticeValidator;
import com.ieltsaitutor.practice.generator.validator.ValidationFinding;
import com.ieltsaitutor.practice.generator.validator.ValidationReport;

@Component
public class SimilarityValidator implements PracticeValidator {

    private final SimilarityPolicyRegistry policyRegistry;
    private final NGramOverlapCalculator nGramCalculator;
    private final ContiguousSequenceDetector sequenceDetector;
    private final EntityRetentionFilter entityFilter;

    public SimilarityValidator(
            SimilarityPolicyRegistry policyRegistry,
            NGramOverlapCalculator nGramCalculator,
            ContiguousSequenceDetector sequenceDetector,
            EntityRetentionFilter entityFilter) {
        this.policyRegistry = policyRegistry;
        this.nGramCalculator = nGramCalculator;
        this.sequenceDetector = sequenceDetector;
        this.entityFilter = entityFilter;
    }

    @Override
    public String name() {
        return "SimilarityValidator";
    }

    @Override
    public ValidationReport validate(RawPracticePackage pkg, BlueprintSchema blueprint, PracticeGenerationSource source) {
        SimilarityPolicy policy = policyRegistry.getActivePolicy();
        List<ValidationFinding> findings = new ArrayList<>();

        if (source == null || source.normalizedContent() == null || source.normalizedContent().isBlank()) {
            return new ValidationReport(name(), policy.version(), ValidationStatus.PASS, findings);
        }

        String generatedPassage = pkg.passage().paragraphs().stream()
                .map(RawPassagePayload.RawParagraphPayload::text)
                .collect(Collectors.joining(" "));
        String sourceText = source.normalizedContent();

        // 1. 4-Gram Overlap Check
        double nGramOverlap = nGramCalculator.calculateOverlap(generatedPassage, sourceText, 4);
        if (nGramOverlap >= policy.maxNGramOverlapFail()) {
            findings.add(new ValidationFinding(
                    "ERR_HIGH_NGRAM_OVERLAP",
                    ValidationStatus.FAIL,
                    "passage",
                    String.format("4-gram token overlap %.2f%% breaches policy failure threshold (%.2f%%)",
                            nGramOverlap * 100.0, policy.maxNGramOverlapFail() * 100.0),
                    "Heuristic token overlap breach; requires substantive passage regeneration"
            ));
        } else if (nGramOverlap >= policy.maxNGramOverlapWarn()) {
            findings.add(new ValidationFinding(
                    "WARN_ELEVATED_NGRAM_OVERLAP",
                    ValidationStatus.WARNING,
                    "passage",
                    String.format("4-gram token overlap %.2f%% exceeds advisory warning threshold (%.2f%%)",
                            nGramOverlap * 100.0, policy.maxNGramOverlapWarn() * 100.0),
                    "Heuristic token overlap advisory; requires explicit reviewer acknowledgment"
            ));
        }

        // 2. Contiguous Run Check
        int maxContiguousRun = sequenceDetector.findLongestMatchingSequence(generatedPassage, sourceText);
        if (maxContiguousRun >= policy.maxContiguousRunFail()) {
            findings.add(new ValidationFinding(
                    "ERR_CONTIGUOUS_SEQUENCE_MATCH",
                    ValidationStatus.FAIL,
                    "passage",
                    String.format("Detected %d contiguous matching words between generated text and source (limit: %d)",
                            maxContiguousRun, policy.maxContiguousRunFail()),
                    "Heuristic contiguous sequence run breach"
            ));
        } else if (maxContiguousRun >= policy.maxContiguousRunWarn()) {
            findings.add(new ValidationFinding(
                    "WARN_CONTIGUOUS_SEQUENCE_MATCH",
                    ValidationStatus.WARNING,
                    "passage",
                    String.format("Detected %d contiguous matching words between generated text and source (warning: %d)",
                            maxContiguousRun, policy.maxContiguousRunWarn()),
                    "Heuristic contiguous sequence advisory"
            ));
        }

        // 3. Entity Retention Filter
        double entityRetention = entityFilter.calculateEntityRetention(sourceText, generatedPassage);
        if (entityRetention >= policy.maxEntityRetentionWarn()) {
            findings.add(new ValidationFinding(
                    "WARN_HIGH_ENTITY_RETENTION",
                    ValidationStatus.WARNING,
                    "passage",
                    String.format("Reused %.1f%% of unique named entities from source (threshold: %.1f%%)",
                            entityRetention * 100.0, policy.maxEntityRetentionWarn() * 100.0),
                    "Heuristic named anchor retention advisory"
            ));
        }

        boolean hasFail = findings.stream().anyMatch(f -> f.severity() == ValidationStatus.FAIL);
        boolean hasWarn = findings.stream().anyMatch(f -> f.severity() == ValidationStatus.WARNING);
        ValidationStatus overall = hasFail ? ValidationStatus.FAIL : (hasWarn ? ValidationStatus.WARNING : ValidationStatus.PASS);

        return new ValidationReport(name(), policy.version(), overall, findings);
    }
}
