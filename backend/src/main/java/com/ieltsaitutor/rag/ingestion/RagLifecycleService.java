package com.ieltsaitutor.rag.ingestion;

import java.util.UUID;

public interface RagLifecycleService {
    void approve(UUID documentId, RightsReviewCommand command);
    void reject(UUID documentId, RightsReviewCommand command);
    void activate(UUID documentId);
    void deactivate(UUID documentId);
}
