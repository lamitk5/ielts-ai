package com.ieltsaitutor.practice;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class SyntheticPracticeCatalog {
    private final List<PracticeSet> sets;

    public SyntheticPracticeCatalog() { this.sets = defaultSets(); }
    SyntheticPracticeCatalog(List<PracticeSet> sets) { this.sets = sets; }

    public static SyntheticPracticeCatalog inMemory() { return new SyntheticPracticeCatalog(defaultSets()); }

    public List<PracticeSet> sets(String skill) {
        String normalized = normalize(skill);
        if (!normalized.equals("reading") && !normalized.equals("listening")) throw new IllegalArgumentException("Skill is not available");
        return sets.stream().filter(set -> set.skill().equalsIgnoreCase(normalized)).toList();
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
                                        "Participants produced more accurate summaries."))),
                new PracticeSet("listening-foundation-01", "Listening", "Listening foundation",
                        "Luyện nghe theo ngữ cảnh với transcript thân thiện để xem lại sau khi trả lời.", List.of(
                                new PracticeQuestion("listening-q1", "What time does the library open?",
                                        List.of("7:30", "8:00", "8:30", "9:00"), "B", "The library opens at 8:00."),
                                new PracticeQuestion("listening-q2", "Which room is reserved?",
                                        List.of("Room 2", "Room 4", "Room 6", "Room 8"), "C", "Room 6 is reserved."))));
    }
}
