package com.ieltsaitutor.practice.generator.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltsaitutor.practice.PracticeParagraph;
import com.ieltsaitutor.practice.PracticePassage;
import com.ieltsaitutor.practice.PracticeQuestion;
import com.ieltsaitutor.practice.PracticeSet;
import com.ieltsaitutor.practice.generator.ai.RawPassagePayload;
import com.ieltsaitutor.practice.generator.ai.RawQuestionPayload;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeSet;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeVersion;
import com.ieltsaitutor.practice.generator.domain.GenerationState;
import com.ieltsaitutor.practice.generator.repository.PracticeGenerationRepository;
import com.ieltsaitutor.practice.generator.repository.PracticeProvenanceRepository;
import com.ieltsaitutor.practice.repository.DatabasePracticeCatalogStore;
import com.ieltsaitutor.rag.domain.Skill;

class PracticeBankHydrationServiceTest {

    private DatabasePracticeCatalogStore catalogStore;
    private PracticeProvenanceRepository provenanceRepository;
    private PracticeGenerationRepository generationRepository;
    private DefaultPracticeBankHydrationService hydrationService;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        catalogStore = mock(DatabasePracticeCatalogStore.class);
        provenanceRepository = mock(PracticeProvenanceRepository.class);
        generationRepository = mock(PracticeGenerationRepository.class);
        mapper = new ObjectMapper();

        hydrationService = new DefaultPracticeBankHydrationService(
                catalogStore, provenanceRepository, generationRepository
        );
    }

    @Test
    void hydrateMapsEntitiesAndSavesCatalogAndProvenance() throws Exception {
        UUID setId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        RawPassagePayload passagePayload = new RawPassagePayload(
                "The History of Clocks",
                List.of(
                        new RawPassagePayload.RawParagraphPayload("p1", "Early humans measured time using the sun and shadows."),
                        new RawPassagePayload.RawParagraphPayload("p2", "Mechanical clocks emerged in the fourteenth century.")
                )
        );

        List<RawQuestionPayload> questionPayloads = List.of(
                new RawQuestionPayload("q1", "TRUE_FALSE_NOT_GIVEN", "Early humans used water clocks only.", List.of("TRUE", "FALSE", "NOT GIVEN"), "FALSE", "sun and shadows", "Sun was used"),
                new RawQuestionPayload("q2", "MULTIPLE_CHOICE", "When did mechanical clocks appear?", List.of("12th", "14th", "16th"), "14th", "fourteenth century", "Fourteenth century")
        );

        GeneratedPracticeSet set = new GeneratedPracticeSet(
                setId, jobId, Skill.READING, "The History of Clocks", versionId,
                GenerationState.APPROVED, null, adminId, Instant.now(), Instant.now(), Instant.now()
        );
        GeneratedPracticeVersion version = new GeneratedPracticeVersion(
                versionId, setId, 1, mapper.writeValueAsString(passagePayload), mapper.writeValueAsString(questionPayloads), "{}", Instant.now()
        );

        when(generationRepository.findJobById(jobId)).thenReturn(Optional.empty());

        String publishedSetId = hydrationService.hydrate(set, version, adminId);

        assertNotNull(publishedSetId);
        assertTrue(publishedSetId.startsWith("reading-synth-"));

        verify(catalogStore).save(argThat(savedSet -> {
            assertEquals(publishedSetId, savedSet.id());
            assertEquals("reading", savedSet.skill());
            assertEquals("The History of Clocks", savedSet.title());
            assertEquals(2, savedSet.questions().size());
            assertNotNull(savedSet.passage());
            assertEquals("The History of Clocks", savedSet.passage().title());
            assertEquals(2, savedSet.passage().paragraphs().size());
            return true;
        }));

        verify(provenanceRepository).save(argThat(prov -> {
            assertEquals(publishedSetId, prov.setId());
            assertEquals(jobId, prov.generationJobId());
            assertEquals(adminId, prov.approverId());
            return true;
        }));
    }
}
