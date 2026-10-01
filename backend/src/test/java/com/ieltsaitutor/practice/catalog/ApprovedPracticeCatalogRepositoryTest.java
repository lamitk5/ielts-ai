package com.ieltsaitutor.practice.catalog;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class ApprovedPracticeCatalogRepositoryTest {

    @Test
    void repositoryQueriesOnlyActivePublications() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        JdbcPracticePublicationRepository repository = new JdbcPracticePublicationRepository(jdbc);

        repository.findActiveBySkill("reading");

        verify(jdbc).query(contains("active = TRUE"), any(MapSqlParameterSource.class), any(org.springframework.jdbc.core.RowMapper.class));
        assertTrue(true);
    }
}
