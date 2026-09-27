package com.ieltsaitutor.practice.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class ApprovedPracticeCatalogService {
    private final PracticePublicationRepository repository;

    public ApprovedPracticeCatalogService(PracticePublicationRepository repository) {
        this.repository = repository;
    }

    public List<PracticePublication> listActive(String skill) {
        if (skill == null || skill.isBlank()) return List.of();
        return repository.findActiveBySkill(skill.trim().toLowerCase());
    }

    public Optional<PracticePublication> findActive(String publishedSetId) {
        return repository.findActiveById(publishedSetId);
    }

    public PracticePublication publishApproved(PracticePublication publication) {
        if (!publication.active()) throw new IllegalArgumentException("Only active approved publications can be exposed");
        return repository.save(publication);
    }
}
