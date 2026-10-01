package com.ieltsaitutor.search;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class PracticeSearchService {
    private final List<PracticeSearchResult> catalog = List.of(
            new PracticeSearchResult("reading-foundation-01", "Reading foundation", "Reading", "Synthetic practice set for main ideas and details.", "/practice/reading"),
            new PracticeSearchResult("listening-foundation-01", "Listening foundation", "Listening", "Synthetic practice set with contextual listening prompts.", "/practice/listening"),
            new PracticeSearchResult("task-1-academic-01", "Academic Writing Task 1", "Writing", "Write a concise summary of a visual prompt.", "/practice/writing"),
            new PracticeSearchResult("speaking-p2-01", "Speaking Part 2", "Speaking", "Plan and save a response to a long-turn prompt.", "/practice/speaking"));

    public List<PracticeSearchResult> search(String query) {
        if (query == null || query.isBlank() || query.trim().length() > 120) throw new IllegalArgumentException("Search query is invalid");
        String normalized = query.trim().toLowerCase();
        List<PracticeSearchResult> matches = catalog.stream().filter(result -> {
            String searchable = (result.title() + " " + result.skill() + " " + result.description()).toLowerCase();
            return normalized.split("\\s+").length == 0 || java.util.Arrays.stream(normalized.split("\\s+")).allMatch(searchable::contains);
        }).toList();
        return matches.isEmpty() ? catalog.stream().filter(result -> normalized.contains(result.skill().toLowerCase())).toList() : matches;
    }
}
