package com.ieltsaitutor.writing;

import java.util.List;
import java.util.UUID;

public interface WritingRepository {
    void save(WritingAssessment assessment);
    default List<WritingAssessment> findByUser(UUID userId) { return List.of(); }
}
