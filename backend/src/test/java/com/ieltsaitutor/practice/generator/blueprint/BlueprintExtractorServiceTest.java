package com.ieltsaitutor.practice.generator.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.service.SourceNormalizationService;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.domain.Skill;

class BlueprintExtractorServiceTest {

    private BlueprintExtractorService service;

    @BeforeEach
    void setUp() {
        DefaultBlueprintCatalog catalog = new DefaultBlueprintCatalog();
        SourceNormalizationService normalizationService = new SourceNormalizationService();
        service = new BlueprintExtractorService(catalog, normalizationService);
    }

    @Test
    void extractsBlueprintFromSourceAndSerializesToJson() {
        String content = "Academic passage discussing marine microplastics and Arctic estuarine dynamics. ".repeat(60);
        PracticeGenerationSource source = new PracticeGenerationSource(
                UUID.randomUUID(), "Arctic Estuarine Dynamics", "PASTED_TEXT", "Researcher",
                RightsStatus.APPROVED, "Open Access", content, "hash", UUID.randomUUID(),
                Instant.now(), Instant.now());

        BlueprintSchema schema = service.extractFromSource(source, Skill.READING, BigDecimal.valueOf(7.5));
        assertNotNull(schema);
        assertEquals(Skill.READING, schema.skill());
        assertEquals(BigDecimal.valueOf(7.5), schema.targetBand());
        assertTrue(schema.itemDistribution().size() >= 3);

        String json = service.toJson(schema);
        assertNotNull(json);
        assertTrue(json.contains("targetBand"));

        BlueprintSchema deserialized = service.fromJson(json);
        assertEquals(schema.blueprintId(), deserialized.blueprintId());
        assertEquals(schema.targetBand(), deserialized.targetBand());
        assertEquals(schema.itemDistribution().size(), deserialized.itemDistribution().size());
    }
}
