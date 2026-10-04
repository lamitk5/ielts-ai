package com.ieltsaitutor.practice.generator.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.exception.SourceRightsException;
import com.ieltsaitutor.practice.generator.repository.PracticeGenerationRepository;
import com.ieltsaitutor.rag.domain.RightsStatus;

@Service
public class SourceRightsGuard {
    private final PracticeGenerationRepository repository;

    public SourceRightsGuard(PracticeGenerationRepository repository) {
        this.repository = repository;
    }

    public PracticeGenerationSource verifySourceCanGenerate(UUID sourceId) {
        if (sourceId == null) {
            throw new SourceRightsException("Source ID must not be null");
        }
        PracticeGenerationSource source = repository.findSourceById(sourceId)
                .orElseThrow(() -> new SourceRightsException("Source not found with ID: " + sourceId));

        if (source.rightsStatus() != RightsStatus.APPROVED) {
            throw new SourceRightsException("Practice generation requires source with rights status APPROVED, but found: "
                    + source.rightsStatus() + " for source: " + source.id());
        }
        return source;
    }
}
