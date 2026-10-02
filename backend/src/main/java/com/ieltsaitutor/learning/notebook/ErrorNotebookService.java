package com.ieltsaitutor.learning.notebook;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ieltsaitutor.learning.intelligence.LearningIntelligenceService;
import com.ieltsaitutor.learning.intelligence.MistakeRecord;

@Service
public class ErrorNotebookService {
    private final LearningIntelligenceService intelligence;
    private final ErrorNotebookAcknowledgementRepository acknowledgements;
    public ErrorNotebookService(LearningIntelligenceService intelligence, ErrorNotebookAcknowledgementRepository acknowledgements) { this.intelligence = intelligence; this.acknowledgements = acknowledgements; }

    public NotebookResponse list(UUID userId, ErrorNotebookQuery query) {
        Map<String, List<MistakeRecord>> groups = new LinkedHashMap<>();
        intelligence.mistakes(userId).stream()
                .filter(m -> query.skill() == null || m.skill().name().equalsIgnoreCase(query.skill()))
                .filter(m -> query.status() == null || m.status().name().equalsIgnoreCase(query.status()))
                .sorted(Comparator.comparing(MistakeRecord::detectedAt).reversed())
                .forEach(m -> groups.computeIfAbsent(key(m), ignored -> new ArrayList<>()).add(m));
        List<ErrorNotebookEntry> all = groups.values().stream().map(this::entry).toList();
        int from = Math.min(query.page() * query.size(), all.size());
        int to = Math.min(from + query.size(), all.size());
        return new NotebookResponse(all.subList(from, to), query.page(), query.size(), all.size());
    }

    private ErrorNotebookEntry entry(List<MistakeRecord> items) {
        MistakeRecord latest = items.get(0), first = items.get(items.size() - 1);
        String state = acknowledgements.findState(latest.userId(), latest.id()).orElse(latest.status().name());
        return new ErrorNotebookEntry(latest.id(), latest.skill(), latest.practiceSetId(), latest.questionId(), latest.learnerAnswerSnapshot(),
                latest.correctAnswerRef(), latest.category(), latest.evidenceText(), items.size(), first.detectedAt(), latest.detectedAt(),
                state, null, items.size() >= 2);
    }

    private String key(MistakeRecord m) { return m.skill() + "|" + m.practiceSetId() + "|" + m.questionId() + "|" + m.category(); }
    public record NotebookResponse(List<ErrorNotebookEntry> entries, int page, int size, int total) { public NotebookResponse { entries = List.copyOf(entries); } }
}
