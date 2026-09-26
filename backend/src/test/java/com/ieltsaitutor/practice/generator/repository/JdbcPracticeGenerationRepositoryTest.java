package com.ieltsaitutor.practice.generator.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeSet;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeVersion;
import com.ieltsaitutor.practice.generator.domain.GenerationState;
import com.ieltsaitutor.practice.generator.domain.GenerationValidationResult;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationBlueprint;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationJob;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.PracticeReviewAction;
import com.ieltsaitutor.practice.generator.domain.ValidationStatus;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.domain.Skill;

class JdbcPracticeGenerationRepositoryTest {

    private NamedParameterJdbcTemplate jdbc;
    private JdbcPracticeGenerationRepository repository;

    @BeforeEach
    void setUp() {
        jdbc = mock(NamedParameterJdbcTemplate.class);
        repository = new JdbcPracticeGenerationRepository(jdbc);
    }

    @Test
    void savesSourceAndQueriesById() {
        UUID sourceId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Instant now = Instant.now();
        PracticeGenerationSource source = new PracticeGenerationSource(
                sourceId, "Article on Coral Reefs", "PASTED_TEXT", "Dr. Marine",
                RightsStatus.APPROVED, "CC-BY 4.0", "Coral reefs are diverse...", "a".repeat(64),
                adminId, now, now);

        repository.saveSource(source);
        verify(jdbc).update(contains("INSERT INTO practice_generation_sources"), any(MapSqlParameterSource.class));

        when(jdbc.query(contains("SELECT * FROM practice_generation_sources WHERE id = :id"),
                any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(source));

        Optional<PracticeGenerationSource> found = repository.findSourceById(sourceId);
        assertEquals(true, found.isPresent());
        assertEquals("Article on Coral Reefs", found.get().title());
        assertEquals(RightsStatus.APPROVED, found.get().rightsStatus());
    }

    @Test
    void savesBlueprintAndQueriesBySkill() {
        UUID bpId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Instant now = Instant.now();
        PracticeGenerationBlueprint blueprint = new PracticeGenerationBlueprint(
                bpId, Skill.READING, "Academic Reading Blueprint 1", BigDecimal.valueOf(7.5),
                "{\"topicCategory\":\"MARINE_BIOLOGY\"}", adminId, now, now);

        repository.saveBlueprint(blueprint);
        verify(jdbc).update(contains("INSERT INTO practice_generation_blueprints"), any(MapSqlParameterSource.class));

        when(jdbc.query(contains("SELECT * FROM practice_generation_blueprints WHERE skill = :skill"),
                any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(blueprint));

        List<PracticeGenerationBlueprint> blueprints = repository.listBlueprints("READING");
        assertEquals(1, blueprints.size());
        assertEquals(Skill.READING, blueprints.get(0).skill());
    }

    @Test
    void savesJobAndUpdatesStatus() {
        UUID jobId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID blueprintId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Instant now = Instant.now();
        PracticeGenerationJob job = new PracticeGenerationJob(
                jobId, sourceId, blueprintId, Skill.READING, "GENERATING", "gemini-1.5-pro",
                "v1.0", null, adminId, now, null, now);

        repository.saveJob(job);
        verify(jdbc).update(contains("INSERT INTO practice_generation_jobs"), any(MapSqlParameterSource.class));

        repository.updateJobStatus(jobId, "COMPLETED", null);
        verify(jdbc).update(contains("UPDATE practice_generation_jobs"), any(MapSqlParameterSource.class));
    }

    @Test
    void savesSetVersionAndValidationResults() {
        UUID setId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        Instant now = Instant.now();

        GeneratedPracticeSet set = new GeneratedPracticeSet(
                setId, jobId, Skill.READING, "Generated Reading Test 1", versionId,
                GenerationState.AUTO_VALIDATING, null, null, null, now, now);
        repository.saveSet(set);
        verify(jdbc).update(contains("INSERT INTO generated_practice_sets"), any(MapSqlParameterSource.class));

        GeneratedPracticeVersion version = new GeneratedPracticeVersion(
                versionId, setId, 1, "{\"title\":\"Passage 1\"}", "[]", "{}", now);
        repository.saveVersion(version);
        verify(jdbc).update(contains("INSERT INTO generated_practice_versions"), any(MapSqlParameterSource.class));

        GenerationValidationResult valResult = new GenerationValidationResult(
                UUID.randomUUID(), versionId, "StructuralValidator", ValidationStatus.PASS, "[]", now);
        repository.saveValidationResult(valResult);
        verify(jdbc).update(contains("INSERT INTO generation_validation_results"), any(MapSqlParameterSource.class));
    }

    @Test
    void savesReviewActionAndUpdatesApprovedState() {
        UUID setId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Instant now = Instant.now();

        PracticeReviewAction action = new PracticeReviewAction(
                UUID.randomUUID(), setId, versionId, adminId, "APPROVE", "Looks great", now);
        repository.saveReviewAction(action);
        verify(jdbc).update(contains("INSERT INTO practice_review_actions"), any(MapSqlParameterSource.class));

        repository.markSetApproved(setId, "reading-gen-coral-reefs", adminId);
        verify(jdbc).update(contains("UPDATE generated_practice_sets"), any(MapSqlParameterSource.class));
    }
}
