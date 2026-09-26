package com.ieltsaitutor.practice;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ieltsaitutor.practice.repository.DatabasePracticeCatalogStore;

@Component
public class SyntheticPracticeCatalog {
    private final List<PracticeSet> defaultSets;
    private final DatabasePracticeCatalogStore dbStore;

    public SyntheticPracticeCatalog() {
        this(defaultSets(), null);
    }

    @Autowired
    public SyntheticPracticeCatalog(DatabasePracticeCatalogStore dbStore) {
        this(defaultSets(), dbStore);
    }

    SyntheticPracticeCatalog(List<PracticeSet> defaultSets, DatabasePracticeCatalogStore dbStore) {
        this.defaultSets = defaultSets != null ? defaultSets : List.of();
        this.dbStore = dbStore;
    }

    public static SyntheticPracticeCatalog inMemory() {
        return new SyntheticPracticeCatalog(defaultSets(), null);
    }

    public static SyntheticPracticeCatalog withStore(DatabasePracticeCatalogStore dbStore) {
        return new SyntheticPracticeCatalog(defaultSets(), dbStore);
    }

    public List<PracticeSet> sets(String skill) {
        String normalized = normalize(skill);
        if (!normalized.equals("reading") && !normalized.equals("listening")) {
            throw new IllegalArgumentException("Skill is not available");
        }
        List<PracticeSet> combined = new ArrayList<>();
        // 1. Default static sets
        defaultSets.stream()
                .filter(set -> set.skill().equalsIgnoreCase(normalized))
                .forEach(combined::add);

        // 2. Approved database sets
        if (dbStore != null) {
            dbStore.findBySkill(normalized).forEach(combined::add);
        }

        return combined;
    }

    public PracticeSet find(String skill, String id) {
        return sets(skill).stream().filter(set -> set.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Practice set not found"));
    }

    private String normalize(String skill) { return skill == null ? "" : skill.trim().toLowerCase(); }

    private static List<PracticeSet> defaultSets() {
        return List.of(
                new PracticeSet("reading-foundation-01", "Reading", "Reading foundation",
                        "Một đoạn đọc ngắn để luyện xác định ý chính và chi tiết.", List.of(
                                new PracticeQuestion("reading-q1", "The passage describes a new study method. What is its main purpose?",
                                        List.of("To replace teachers", "To improve recall", "To reduce reading", "To test speed"), "B",
                                        "The passage focuses on improving recall."),
                                new PracticeQuestion("reading-q2", "Which result did the researchers observe?",
                                        List.of("More accurate summaries", "Longer exams", "Fewer participants", "Higher costs"), "A",
                                        "Participants produced more accurate summaries.")),
                        new PracticePassage("Learning Through Spaced Practice", List.of(
                                new PracticeParagraph("reading-foundation-01-p1",
                                        "Researchers tested spaced retrieval practice with students. Instead of replacing teachers or reducing reading, the method asked learners to revisit key ideas at intervals. Its main purpose was to improve recall."),
                                new PracticeParagraph("reading-foundation-01-p2",
                                        "After two weeks, researchers compared written summaries. Participants who used spaced retrieval produced more accurate summaries than those who reread the text only once.")))),
                new PracticeSet("listening-foundation-01", "Listening", "Listening foundation",
                        "Luyện nghe theo ngữ cảnh với transcript thân thiện để xem lại sau khi trả lời.", List.of(
                                new PracticeQuestion("listening-q1", "What time does the library open?",
                                        List.of("7:30", "8:00", "8:30", "9:00"), "B", "The library opens at 8:00."),
                                new PracticeQuestion("listening-q2", "Which room is reserved?",
                                        List.of("Room 2", "Room 4", "Room 6", "Room 8"), "C", "Room 6 is reserved."))));
    }
}
