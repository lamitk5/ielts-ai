package com.ieltsaitutor.practice.generator.blueprint;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationBlueprint;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.service.SourceNormalizationService;
import com.ieltsaitutor.rag.domain.Skill;

@Service
public class BlueprintExtractorService {

    private final ObjectMapper mapper;
    private final DefaultBlueprintCatalog catalog;
    private final SourceNormalizationService normalizationService;

    public BlueprintExtractorService(DefaultBlueprintCatalog catalog, SourceNormalizationService normalizationService) {
        this.catalog = catalog;
        this.normalizationService = normalizationService;
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public BlueprintSchema extractFromSource(PracticeGenerationSource source, Skill skill, BigDecimal targetBand) {
        if (skill == null) skill = Skill.READING;
        if (targetBand == null) targetBand = BigDecimal.valueOf(7.0);

        int wordCount = normalizationService.countWords(source.normalizedContent());
        int estimatedParagraphs = Math.max(4, Math.min(8, wordCount / 120));

        List<ItemDistributionSpec> distributions;
        if (targetBand.doubleValue() >= 8.0) {
            distributions = List.of(
                    new ItemDistributionSpec("MULTIPLE_CHOICE", 4, List.of(1, 2, 3), "INFERENCE_AND_TONE"),
                    new ItemDistributionSpec("YES_NO_NOT_GIVEN", 3, List.of(3, 4, 5), "WRITER_VIEWS_AND_CLAIMS"),
                    new ItemDistributionSpec("SUMMARY_COMPLETION", 3, List.of(5, 6), "SYNTACTIC_AND_LEXICAL_SYNONYMY")
            );
        } else if (targetBand.doubleValue() >= 7.0) {
            distributions = List.of(
                    new ItemDistributionSpec("MATCHING_HEADINGS", 4, List.of(1, 2, 3, 4), "PARAGRAPH_MAIN_IDEA"),
                    new ItemDistributionSpec("TRUE_FALSE_NOT_GIVEN", 3, List.of(4, 5), "FACTUAL_DETAIL_VERIFICATION"),
                    new ItemDistributionSpec("SUMMARY_COMPLETION", 3, List.of(5, 6), "SYNTACTIC_AND_LEXICAL_SYNONYMY")
            );
        } else {
            distributions = List.of(
                    new ItemDistributionSpec("TRUE_FALSE_NOT_GIVEN", 5, List.of(1, 2, 3), "FACTUAL_DETAIL_VERIFICATION"),
                    new ItemDistributionSpec("MULTIPLE_CHOICE", 3, List.of(3, 4), "FACTUAL_AND_MAIN_IDEA"),
                    new ItemDistributionSpec("SUMMARY_COMPLETION", 2, List.of(4, 5), "SYNTACTIC_AND_LEXICAL_SYNONYMY")
            );
        }

        String topicCategory = source.title() != null && !source.title().isBlank()
                ? source.title().replaceAll("[^a-zA-Z0-9_ ]", "").toUpperCase().replace(' ', '_')
                : "GENERAL_ACADEMIC";

        return new BlueprintSchema(
                "bp-" + UUID.randomUUID().toString().substring(0, 8),
                skill,
                targetBand,
                topicCategory,
                new PassageStructureSpec(wordCount, estimatedParagraphs, "EXPOSITORY_AND_ANALYTICAL", "ACADEMIC_CEFR_C1"),
                distributions,
                new ReasoningRulesSpec(true, true, 3)
        );
    }

    public String toJson(BlueprintSchema schema) {
        try {
            return mapper.writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize BlueprintSchema to JSON", e);
        }
    }

    public BlueprintSchema fromJson(String json) {
        try {
            return mapper.readValue(json, BlueprintSchema.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize BlueprintSchema from JSON", e);
        }
    }
}
