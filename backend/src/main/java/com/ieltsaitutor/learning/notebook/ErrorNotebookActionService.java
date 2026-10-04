package com.ieltsaitutor.learning.notebook;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.auth.AuthException;

@Service
public class ErrorNotebookActionService {
    private final com.ieltsaitutor.learning.intelligence.LearningIntelligenceService intelligence;
    private final ErrorNotebookAcknowledgementRepository acknowledgements;
    public ErrorNotebookActionService(com.ieltsaitutor.learning.intelligence.LearningIntelligenceService intelligence, ErrorNotebookAcknowledgementRepository acknowledgements) { this.intelligence = intelligence; this.acknowledgements = acknowledgements; }
    public void acknowledge(UUID userId, UUID mistakeId) {
        if (userId == null || mistakeId == null || intelligence.mistakes(userId).stream().noneMatch(m -> mistakeId.equals(m.id()))) {
            throw new AuthException("NOTEBOOK_NOT_FOUND", HttpStatus.NOT_FOUND, "Mục lỗi sai không tồn tại.");
        }
        acknowledgements.acknowledge(userId, mistakeId);
    }
}
