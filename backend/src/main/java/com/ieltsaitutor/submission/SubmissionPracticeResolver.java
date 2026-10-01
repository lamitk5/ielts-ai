package com.ieltsaitutor.submission;

import org.springframework.stereotype.Service;

import com.ieltsaitutor.practice.catalog.ApprovedPracticeCatalogService;
import com.ieltsaitutor.practice.catalog.PracticePublication;

@Service
public class SubmissionPracticeResolver {
    private final ApprovedPracticeCatalogService catalog;

    public SubmissionPracticeResolver(ApprovedPracticeCatalogService catalog) {
        this.catalog = catalog;
    }

    public ResolvedPracticeVersion resolve(String publishedSetId, String requestedSkill) {
        PracticePublication publication = catalog.findActive(publishedSetId)
                .filter(item -> requestedSkill != null && item.skill().equalsIgnoreCase(requestedSkill.trim()))
                .orElseThrow(() -> new SubmissionConflictException("Practice is not available for submission"));
        return new ResolvedPracticeVersion(publication.publishedSetId(), publication.skill().toUpperCase(),
                publication.generatedVersionId().toString(), publication.publicationRevision());
    }
}
