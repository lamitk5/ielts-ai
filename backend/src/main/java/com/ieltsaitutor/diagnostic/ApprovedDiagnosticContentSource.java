package com.ieltsaitutor.diagnostic;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.ieltsaitutor.learning.intelligence.Skill;
import com.ieltsaitutor.practice.catalog.ApprovedPracticeCatalogService;
import com.ieltsaitutor.practice.catalog.PracticePublication;

/** Reads approved diagnostic content from the Phase 4 publication catalog. */
@Component
public class ApprovedDiagnosticContentSource implements DiagnosticContentSource {
    private final ApprovedPracticeCatalogService catalog;

    public ApprovedDiagnosticContentSource(ApprovedPracticeCatalogService catalog) {
        this.catalog = catalog;
    }

    @Override
    public Optional<DiagnosticContentPin> newestApproved(Skill skill) {
        return catalog.listActive(skill.name().toLowerCase()).stream()
                .findFirst()
                .map(ApprovedDiagnosticContentSource::toPin);
    }

    private static DiagnosticContentPin toPin(PracticePublication publication) {
        if (publication.generatedVersionId() == null) {
            return null;
        }
        return DiagnosticContentPin.of(publication.publishedSetId(),
                publication.generatedVersionId().toString(), publication.publicationRevision(),
                publication.provenanceReference());
    }
}