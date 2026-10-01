package com.ieltsaitutor.practice.generator.repository;

import java.util.Optional;
import com.ieltsaitutor.practice.generator.service.PracticeProvenanceRecord;

public interface PracticeProvenanceRepository {
    PracticeProvenanceRecord save(PracticeProvenanceRecord record);
    Optional<PracticeProvenanceRecord> findBySetId(String setId);
}
