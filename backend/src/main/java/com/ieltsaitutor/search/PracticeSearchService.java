package com.ieltsaitutor.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PracticeSearchService {
    private static final int MAX_QUERY_LENGTH = 120;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final NamedParameterJdbcTemplate jdbc;

    private final List<PracticeSearchResult> staticCatalog = List.of(
            new PracticeSearchResult("reading-foundation-01", "Reading foundation", "Reading",
                    "Synthetic practice set for main ideas and details.", "/practice/reading", "PRACTICE_SET", "Bài luyện"),
            new PracticeSearchResult("listening-foundation-01", "Listening foundation", "Listening",
                    "Synthetic practice set with contextual listening prompts.", "/practice/listening", "PRACTICE_SET", "Bài luyện"),
            new PracticeSearchResult("task-1-academic-01", "Academic Writing Task 1", "Writing",
                    "Write a concise summary of a visual prompt.", "/practice/writing", "PRACTICE_SET", "Bài luyện"),
            new PracticeSearchResult("speaking-p2-01", "Speaking Part 2", "Speaking",
                    "Plan and save a response to a long-turn prompt.", "/practice/speaking", "PRACTICE_SET", "Bài luyện"));

    private final List<PracticeSearchResult> writingPrompts = List.of(
            new PracticeSearchResult("task-1-academic-01", "Academic Task 1", "Writing",
                    "Summarise the information in a chart or process.", "/practice/writing", "WRITING_PROMPT", "Writing Prompt"),
            new PracticeSearchResult("task-2-opinion-01", "Essay Task 2", "Writing",
                    "Discuss both views and give your own opinion.", "/practice/writing", "WRITING_PROMPT", "Writing Prompt"));

    private final List<PracticeSearchResult> speakingTopics = List.of(
            new PracticeSearchResult("speaking-p1-01", "Speaking Part 1", "Speaking",
                    "Do you enjoy reading in your free time?", "/practice/speaking", "SPEAKING_TOPIC", "Speaking Topic"),
            new PracticeSearchResult("speaking-p2-01", "Speaking Part 2", "Speaking",
                    "Describe a place where you like to study.", "/practice/speaking", "SPEAKING_TOPIC", "Speaking Topic"),
            new PracticeSearchResult("speaking-p3-01", "Speaking Part 3", "Speaking",
                    "How can cities support lifelong learning?", "/practice/speaking", "SPEAKING_TOPIC", "Speaking Topic"));

    public PracticeSearchService() {
        this(null);
    }

    @Autowired
    public PracticeSearchService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PracticeSearchResult> search(String query) {
        return search(query, null, null, null, 0, DEFAULT_PAGE_SIZE);
    }

    public List<PracticeSearchResult> search(String query, UUID userId, String skillFilter, String typeFilter, int page, int size) {
        if (query == null || query.isBlank() || query.trim().length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("Search query is invalid");
        }

        String normalized = query.trim().toLowerCase(Locale.ROOT);
        String[] tokens = normalized.split("\\s+");
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.max(1, Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE));

        List<PracticeSearchResult> results = new ArrayList<>();

        // 1. Approved Published Practices from DB (if available)
        if (jdbc != null) {
            results.addAll(searchApprovedPublicationsFromDb(normalized));
        }

        // 2. Static approved catalog items
        for (PracticeSearchResult item : staticCatalog) {
            if (matches(item, tokens, normalized)) {
                // Avoid duplicating items already found from DB
                if (results.stream().noneMatch(r -> r.id().equalsIgnoreCase(item.id()))) {
                    results.add(item);
                }
            }
        }

        // 3. Writing Prompts
        for (PracticeSearchResult prompt : writingPrompts) {
            if (matches(prompt, tokens, normalized)) {
                results.add(prompt);
            }
        }

        // 4. Speaking Topics
        for (PracticeSearchResult topic : speakingTopics) {
            if (matches(topic, tokens, normalized)) {
                results.add(topic);
            }
        }

        // 5. Current learner's OWN submission history ONLY (when userId is provided)
        if (userId != null && jdbc != null) {
            results.addAll(searchOwnedSubmissions(userId, normalized));
        }

        // Fallback matching by skill keyword if no token matches were found
        if (results.isEmpty()) {
            for (PracticeSearchResult item : staticCatalog) {
                if (normalized.contains(item.skill().toLowerCase(Locale.ROOT))) {
                    results.add(item);
                }
            }
        }

        // Apply skill filter if provided
        if (skillFilter != null && !skillFilter.isBlank()) {
            String sf = skillFilter.trim().toLowerCase(Locale.ROOT);
            results = results.stream().filter(r -> r.skill().equalsIgnoreCase(sf)).toList();
        }

        // Apply type filter if provided
        if (typeFilter != null && !typeFilter.isBlank()) {
            String tf = typeFilter.trim().toUpperCase(Locale.ROOT);
            results = results.stream().filter(r -> r.resultType().equalsIgnoreCase(tf)).toList();
        }

        // Pagination
        int fromIndex = boundedPage * boundedSize;
        if (fromIndex >= results.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(fromIndex + boundedSize, results.size());
        return results.subList(fromIndex, toIndex);
    }

    private boolean matches(PracticeSearchResult item, String[] tokens, String rawQuery) {
        String searchable = (item.id() + " " + item.title() + " " + item.skill() + " " + item.description() + " " + item.typeLabel()).toLowerCase(Locale.ROOT);
        if (searchable.contains(rawQuery)) {
            return true;
        }
        if (tokens.length == 0) {
            return false;
        }
        return Arrays.stream(tokens).allMatch(searchable::contains);
    }

    private List<PracticeSearchResult> searchApprovedPublicationsFromDb(String normalized) {
        try {
            String sql = """
                    SELECT p.published_set_id, s.title, p.skill
                    FROM practice_catalog_publications p
                    JOIN generated_practice_sets s ON p.generated_set_id = s.id
                    WHERE p.active = TRUE
                      AND s.state = 'APPROVED'
                      AND (LOWER(s.title) LIKE :likePattern OR LOWER(p.skill) LIKE :likePattern OR LOWER(p.published_set_id) LIKE :likePattern)
                    ORDER BY p.published_at DESC
                    LIMIT 30
                    """;
            MapSqlParameterSource params = new MapSqlParameterSource("likePattern", "%" + normalized + "%");
            return jdbc.query(sql, params, (rs, rowNum) -> {
                String publishedSetId = rs.getString("published_set_id");
                String title = rs.getString("title");
                String skill = rs.getString("skill");
                String formattedSkill = skill.substring(0, 1).toUpperCase(Locale.ROOT) + skill.substring(1).toLowerCase(Locale.ROOT);
                String route = "/practice/" + skill.toLowerCase(Locale.ROOT) + "/" + publishedSetId;
                return new PracticeSearchResult(publishedSetId, title, formattedSkill,
                        "Bài luyện IELTS " + formattedSkill + " đã được kiểm duyệt.", route, "PRACTICE_SET", "Bài luyện");
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<PracticeSearchResult> searchOwnedSubmissions(UUID userId, String normalized) {
        List<PracticeSearchResult> submissions = new ArrayList<>();
        try {
            // Practice submissions (reading, listening, mock test, etc.)
            String psSql = """
                    SELECT id, skill, practice_id, published_set_id, status, submitted_at, created_at
                    FROM practice_submissions
                    WHERE user_id = :userId
                      AND (LOWER(skill) LIKE :likePattern OR LOWER(practice_id) LIKE :likePattern OR LOWER(published_set_id) LIKE :likePattern OR :isHistoryQuery = TRUE)
                    ORDER BY created_at DESC
                    LIMIT 15
                    """;
            boolean isHistoryQuery = normalized.contains("lịch sử") || normalized.contains("history") || normalized.contains("bài làm") || normalized.contains("submission");
            MapSqlParameterSource psParams = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("likePattern", "%" + normalized + "%")
                    .addValue("isHistoryQuery", isHistoryQuery);

            jdbc.query(psSql, psParams, rs -> {
                String id = rs.getString("id");
                String skill = rs.getString("skill");
                String practiceId = rs.getString("practice_id");
                String status = rs.getString("status");
                String formattedSkill = skill.substring(0, 1).toUpperCase(Locale.ROOT) + skill.substring(1).toLowerCase(Locale.ROOT);
                submissions.add(new PracticeSearchResult(
                        id,
                        "Lịch sử bài làm: " + formattedSkill + " (" + practiceId + ")",
                        formattedSkill,
                        "Trạng thái: " + status + " — Bài làm của bạn.",
                        "/practice/results/" + id,
                        "SUBMISSION_HISTORY",
                        "Lịch sử bài làm"
                ));
            });

            // Writing submissions
            String wsSql = """
                    SELECT id, task_id, assessment_status, created_at
                    FROM writing_submissions
                    WHERE user_id = :userId
                      AND (LOWER(task_id) LIKE :likePattern OR LOWER(assessment_status) LIKE :likePattern OR :isHistoryQuery = TRUE)
                    ORDER BY created_at DESC
                    LIMIT 10
                    """;
            jdbc.query(wsSql, psParams, rs -> {
                String id = rs.getString("id");
                String taskId = rs.getString("task_id");
                String status = rs.getString("assessment_status");
                submissions.add(new PracticeSearchResult(
                        id,
                        "Bài viết đã nộp: " + taskId,
                        "Writing",
                        "Trạng thái: " + status + " — Bài làm Writing của bạn.",
                        "/practice/writing",
                        "SUBMISSION_HISTORY",
                        "Lịch sử bài làm"
                ));
            });

            // Speaking attempts
            String ssSql = """
                    SELECT id, prompt_id, attempt_status, created_at
                    FROM speaking_attempts
                    WHERE user_id = :userId
                      AND (LOWER(prompt_id) LIKE :likePattern OR LOWER(attempt_status) LIKE :likePattern OR :isHistoryQuery = TRUE)
                    ORDER BY created_at DESC
                    LIMIT 10
                    """;
            jdbc.query(ssSql, psParams, rs -> {
                String id = rs.getString("id");
                String promptId = rs.getString("prompt_id");
                String status = rs.getString("attempt_status");
                submissions.add(new PracticeSearchResult(
                        id,
                        "Lượt luyện Speaking: " + promptId,
                        "Speaking",
                        "Trạng thái: " + status + " — Lượt nói của bạn.",
                        "/practice/speaking",
                        "SUBMISSION_HISTORY",
                        "Lịch sử bài làm"
                ));
            });
        } catch (Exception e) {
            // DB errors gracefully return empty submission results
        }
        return submissions;
    }
}
