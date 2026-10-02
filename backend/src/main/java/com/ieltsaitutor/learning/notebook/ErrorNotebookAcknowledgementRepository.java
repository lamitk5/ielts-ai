package com.ieltsaitutor.learning.notebook;

import java.util.Optional;
import java.util.UUID;

public interface ErrorNotebookAcknowledgementRepository {
    Optional<String> findState(UUID userId, UUID mistakeId);
    void acknowledge(UUID userId, UUID mistakeId);
}
