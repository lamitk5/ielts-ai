package com.ieltsaitutor.search;

public record PracticeSearchResult(
        String id,
        String title,
        String skill,
        String description,
        String route,
        String resultType,
        String typeLabel) {

    public PracticeSearchResult(String id, String title, String skill, String description, String route) {
        this(id, title, skill, description, route, "PRACTICE_SET", "Bài luyện");
    }

    public PracticeSearchResult {
        resultType = resultType == null || resultType.isBlank() ? "PRACTICE_SET" : resultType;
        typeLabel = typeLabel == null || typeLabel.isBlank() ? "Bài luyện" : typeLabel;
    }
}
