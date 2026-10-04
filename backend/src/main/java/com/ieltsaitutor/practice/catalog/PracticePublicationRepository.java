package com.ieltsaitutor.practice.catalog;

import java.util.List;
import java.util.Optional;

public interface PracticePublicationRepository {
    PracticePublication save(PracticePublication publication);
    List<PracticePublication> findActiveBySkill(String skill);
    Optional<PracticePublication> findActiveById(String publishedSetId);
    void deactivate(String publishedSetId);
}
