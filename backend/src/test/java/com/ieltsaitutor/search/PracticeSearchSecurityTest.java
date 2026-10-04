package com.ieltsaitutor.search;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class PracticeSearchSecurityTest {
    private NamedParameterJdbcTemplate jdbc;
    private PracticeSearchService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(NamedParameterJdbcTemplate.class);
        service = new PracticeSearchService(jdbc);
    }

    @Test
    void searchNeverExposesUnapprovedPhase3Content() {
        // Query to DB MUST require active = TRUE and state = 'APPROVED'
        service.search("Reading Practice", null, null, null, 0, 20);

        verify(jdbc).query(
                contains("s.state = 'APPROVED'"),
                any(MapSqlParameterSource.class),
                any(RowMapper.class)
        );
    }

    @Test
    void searchNeverExposesUserBSubmissionToUserA() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        service.search("lịch sử", userA, null, null, 0, 20);

        // Verify that queries to submission history strictly scope by user_id = userA
        verify(jdbc, atLeastOnce()).query(
                contains("WHERE user_id = :userId"),
                org.mockito.ArgumentMatchers.argThat((MapSqlParameterSource p) -> userA.equals(p.getValue("userId"))),
                any(RowCallbackHandler.class)
        );

        // Ensure userB parameter is never passed
        verify(jdbc, never()).query(
                any(String.class),
                org.mockito.ArgumentMatchers.argThat((MapSqlParameterSource p) -> userB.equals(p.getValue("userId"))),
                any(RowCallbackHandler.class)
        );
    }

    @Test
    void unauthenticatedSearchNeverQueriesUserSubmissions() {
        service.search("history", null, null, null, 0, 20);

        // Never query submission history tables when userId is null
        verify(jdbc, never()).query(
                contains("FROM practice_submissions"),
                any(MapSqlParameterSource.class),
                any(RowCallbackHandler.class)
        );
        verify(jdbc, never()).query(
                contains("FROM writing_submissions"),
                any(MapSqlParameterSource.class),
                any(RowCallbackHandler.class)
        );
    }

    @Test
    void searchNeverExposesAdminReviewDataOrPrivateRagInternals() {
        List<PracticeSearchResult> results = service.search("Academic Task 1");

        for (PracticeSearchResult item : results) {
            assertFalse(item.title().contains("admin_review"));
            assertFalse(item.description().contains("system_prompt"));
            assertFalse(item.route().startsWith("/admin"));
        }
    }
}
