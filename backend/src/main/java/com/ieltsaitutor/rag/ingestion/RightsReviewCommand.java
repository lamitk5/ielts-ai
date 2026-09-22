package com.ieltsaitutor.rag.ingestion;

public record RightsReviewCommand(String rightsNote) {
    public RightsReviewCommand {
        if (rightsNote == null || rightsNote.isBlank()) {
            throw new IllegalArgumentException("A rights note is required");
        }
        rightsNote = rightsNote.trim();
    }
}
