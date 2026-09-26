package com.ieltsaitutor.practice.generator.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.practice.generator.domain.PracticeReviewAction;
import com.ieltsaitutor.practice.generator.repository.PracticeGenerationRepository;

@Service
public class PracticeAuditService {

    private final PracticeGenerationRepository repository;

    public PracticeAuditService(PracticeGenerationRepository repository) {
        this.repository = repository;
    }

    public PracticeReviewAction recordAction(UUID setId, UUID versionId, UUID adminId, String action, String notes) {
        PracticeReviewAction reviewAction = new PracticeReviewAction(
                UUID.randomUUID(),
                setId,
                versionId,
                adminId,
                action,
                notes != null ? notes : "",
                Instant.now()
        );
        return repository.saveReviewAction(reviewAction);
    }

    public List<PracticeReviewAction> listActions(UUID setId) {
        return repository.listReviewActionsForSet(setId);
    }
}
