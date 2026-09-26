package com.ieltsaitutor.practice.generator.service;

import java.util.List;
import java.util.UUID;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeSet;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeVersion;
import com.ieltsaitutor.practice.generator.domain.PracticeReviewAction;
import com.ieltsaitutor.practice.generator.dto.GeneratedSetReviewPayload;
import com.ieltsaitutor.practice.generator.dto.ReviewActionRequest;
import com.ieltsaitutor.practice.generator.dto.ReviewActionResponse;

public interface PracticeReviewService {
    ReviewActionResponse executeReview(UUID setId, ReviewActionRequest request, UUID adminId);
    GeneratedSetReviewPayload getReviewPayload(UUID setId);
    List<GeneratedPracticeSet> listSets(String stateFilter);
    List<GeneratedPracticeVersion> getVersionHistory(UUID setId);
    List<PracticeReviewAction> getAuditHistory(UUID setId);
}
