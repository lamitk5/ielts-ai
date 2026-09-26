package com.ieltsaitutor.practice.generator.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltsaitutor.practice.PracticeParagraph;
import com.ieltsaitutor.practice.PracticePassage;
import com.ieltsaitutor.practice.PracticeQuestion;
import com.ieltsaitutor.practice.PracticeSet;
import com.ieltsaitutor.practice.generator.ai.RawPassagePayload;
import com.ieltsaitutor.practice.generator.ai.RawQuestionPayload;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeSet;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeVersion;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationJob;
import com.ieltsaitutor.practice.generator.repository.PracticeGenerationRepository;
import com.ieltsaitutor.practice.generator.repository.PracticeProvenanceRepository;
import com.ieltsaitutor.practice.repository.DatabasePracticeCatalogStore;

@Service
public class DefaultPracticeBankHydrationService implements PracticeBankHydrationService {

    private final DatabasePracticeCatalogStore catalogStore;
    private final PracticeProvenanceRepository provenanceRepository;
    private final PracticeGenerationRepository generationRepository;
    private final ObjectMapper mapper;

    public DefaultPracticeBankHydrationService(
            DatabasePracticeCatalogStore catalogStore,
            PracticeProvenanceRepository provenanceRepository,
            PracticeGenerationRepository generationRepository) {
        this.catalogStore = catalogStore;
        this.provenanceRepository = provenanceRepository;
        this.generationRepository = generationRepository;
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    @Transactional
    public String hydrate(GeneratedPracticeSet set, GeneratedPracticeVersion version, UUID adminId) {
        if (set == null) {
            throw new IllegalArgumentException("GeneratedPracticeSet cannot be null");
        }
        if (version == null) {
            throw new IllegalArgumentException("GeneratedPracticeVersion cannot be null");
        }

        String publishedSetId = (set.publishedSetId() != null && !set.publishedSetId().isBlank())
                ? set.publishedSetId()
                : (set.skill().name().toLowerCase() + "-synth-" + set.id().toString().substring(0, 8));

        RawPassagePayload rawPassage = parsePassage(version.passageContent());
        List<RawQuestionPayload> rawQuestions = parseQuestions(version.questionsPayload());

        List<PracticeParagraph> paragraphs = rawPassage.paragraphs().stream()
                .map(p -> new PracticeParagraph(
                        (p.id() != null && !p.id().isBlank()) ? (publishedSetId + "-" + p.id()) : (publishedSetId + "-p1"),
                        p.text()
                ))
                .toList();
        PracticePassage passage = new PracticePassage(rawPassage.title(), paragraphs);

        List<PracticeQuestion> questions = rawQuestions.stream()
                .map(q -> new PracticeQuestion(
                        publishedSetId + "-" + q.id(),
                        q.prompt(),
                        q.options(),
                        q.answerKey(),
                        q.explanation() != null ? q.explanation() : ""
                ))
                .toList();

        PracticeSet studentSet = new PracticeSet(
                publishedSetId,
                set.skill().name().toLowerCase(),
                set.title(),
                "AI-generated practice set curated and approved by editorial review",
                questions,
                passage
        );

        catalogStore.save(studentSet);

        UUID bpId = null;
        if (set.jobId() != null) {
            PracticeGenerationJob job = generationRepository.findJobById(set.jobId()).orElse(null);
            if (job != null) {
                bpId = job.blueprintId();
            }
        }

        PracticeProvenanceRecord prov = new PracticeProvenanceRecord(
                publishedSetId,
                set.jobId(),
                bpId,
                adminId,
                Instant.now(),
                "{\"sourceSetId\":\"" + set.id() + "\",\"versionNumber\":" + version.versionNumber() + "}"
        );
        provenanceRepository.save(prov);

        return publishedSetId;
    }

    private RawPassagePayload parsePassage(String json) {
        try {
            return mapper.readValue(json, RawPassagePayload.class);
        } catch (JsonProcessingException e) {
            return new RawPassagePayload("Reading Passage", List.of(new RawPassagePayload.RawParagraphPayload("p1", json)));
        }
    }

    private List<RawQuestionPayload> parseQuestions(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<RawQuestionPayload>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
