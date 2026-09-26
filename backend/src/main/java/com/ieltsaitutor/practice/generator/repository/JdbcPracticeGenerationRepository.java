package com.ieltsaitutor.practice.generator.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

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

@Repository
public class JdbcPracticeGenerationRepository implements PracticeGenerationRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPracticeGenerationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Sources
    @Override
    public PracticeGenerationSource saveSource(PracticeGenerationSource source) {
        jdbc.update("""
                INSERT INTO practice_generation_sources
                (id, title, source_type, author, rights_status, license_note, normalized_content, checksum, created_by, created_at, updated_at)
                VALUES (:id, :title, :sourceType, :author, :rightsStatus, :licenseNote, :normalizedContent, :checksum, :createdBy, :createdAt, :updatedAt)
                ON CONFLICT (id) DO UPDATE SET
                    title = EXCLUDED.title,
                    source_type = EXCLUDED.source_type,
                    author = EXCLUDED.author,
                    rights_status = EXCLUDED.rights_status,
                    license_note = EXCLUDED.license_note,
                    normalized_content = EXCLUDED.normalized_content,
                    checksum = EXCLUDED.checksum,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", source.id())
                .addValue("title", source.title())
                .addValue("sourceType", source.sourceType())
                .addValue("author", source.author())
                .addValue("rightsStatus", source.rightsStatus().name())
                .addValue("licenseNote", source.licenseNote())
                .addValue("normalizedContent", source.normalizedContent())
                .addValue("checksum", source.checksum())
                .addValue("createdBy", source.createdBy())
                .addValue("createdAt", Timestamp.from(source.createdAt()))
                .addValue("updatedAt", Timestamp.from(source.updatedAt())));
        return source;
    }

    @Override
    public Optional<PracticeGenerationSource> findSourceById(UUID id) {
        return jdbc.query("SELECT * FROM practice_generation_sources WHERE id = :id",
                new MapSqlParameterSource("id", id), SOURCE_MAPPER).stream().findFirst();
    }

    @Override
    public Optional<PracticeGenerationSource> findSourceByChecksum(String checksum) {
        return jdbc.query("SELECT * FROM practice_generation_sources WHERE checksum = :checksum",
                new MapSqlParameterSource("checksum", checksum), SOURCE_MAPPER).stream().findFirst();
    }

    @Override
    public List<PracticeGenerationSource> listSources(RightsStatus filterRights) {
        if (filterRights != null) {
            return jdbc.query("SELECT * FROM practice_generation_sources WHERE rights_status = :status ORDER BY created_at DESC",
                    new MapSqlParameterSource("status", filterRights.name()), SOURCE_MAPPER);
        }
        return jdbc.query("SELECT * FROM practice_generation_sources ORDER BY created_at DESC", Map.of(), SOURCE_MAPPER);
    }

    @Override
    public void updateSourceRights(UUID sourceId, RightsStatus status, String licenseNote) {
        jdbc.update("""
                UPDATE practice_generation_sources
                SET rights_status = :status, license_note = :licenseNote, updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", sourceId)
                .addValue("status", status.name())
                .addValue("licenseNote", licenseNote != null ? licenseNote : ""));
    }

    // Blueprints
    @Override
    public PracticeGenerationBlueprint saveBlueprint(PracticeGenerationBlueprint blueprint) {
        jdbc.update("""
                INSERT INTO practice_generation_blueprints
                (id, skill, title, target_band, blueprint_schema, created_by, created_at, updated_at)
                VALUES (:id, :skill, :title, :targetBand, :blueprintSchema::jsonb, :createdBy, :createdAt, :updatedAt)
                ON CONFLICT (id) DO UPDATE SET
                    skill = EXCLUDED.skill,
                    title = EXCLUDED.title,
                    target_band = EXCLUDED.target_band,
                    blueprint_schema = EXCLUDED.blueprint_schema,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", blueprint.id())
                .addValue("skill", blueprint.skill().name())
                .addValue("title", blueprint.title())
                .addValue("targetBand", blueprint.targetBand())
                .addValue("blueprintSchema", blueprint.blueprintSchema())
                .addValue("createdBy", blueprint.createdBy())
                .addValue("createdAt", Timestamp.from(blueprint.createdAt()))
                .addValue("updatedAt", Timestamp.from(blueprint.updatedAt())));
        return blueprint;
    }

    @Override
    public Optional<PracticeGenerationBlueprint> findBlueprintById(UUID id) {
        return jdbc.query("SELECT * FROM practice_generation_blueprints WHERE id = :id",
                new MapSqlParameterSource("id", id), BLUEPRINT_MAPPER).stream().findFirst();
    }

    @Override
    public List<PracticeGenerationBlueprint> listBlueprints(String skill) {
        if (skill != null && !skill.isBlank()) {
            return jdbc.query("SELECT * FROM practice_generation_blueprints WHERE skill = :skill ORDER BY created_at DESC",
                    new MapSqlParameterSource("skill", skill.toUpperCase()), BLUEPRINT_MAPPER);
        }
        return jdbc.query("SELECT * FROM practice_generation_blueprints ORDER BY created_at DESC", Map.of(), BLUEPRINT_MAPPER);
    }

    // Jobs
    @Override
    public PracticeGenerationJob saveJob(PracticeGenerationJob job) {
        jdbc.update("""
                INSERT INTO practice_generation_jobs
                (id, source_id, blueprint_id, skill, status, model_id, prompt_template_version, error_message, created_by, started_at, completed_at, created_at)
                VALUES (:id, :sourceId, :blueprintId, :skill, :status, :modelId, :promptTemplateVersion, :errorMessage, :createdBy, :startedAt, :completedAt, :createdAt)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    error_message = EXCLUDED.error_message,
                    started_at = EXCLUDED.started_at,
                    completed_at = EXCLUDED.completed_at
                """, new MapSqlParameterSource()
                .addValue("id", job.id())
                .addValue("sourceId", job.sourceId())
                .addValue("blueprintId", job.blueprintId())
                .addValue("skill", job.skill().name())
                .addValue("status", job.status())
                .addValue("modelId", job.modelId())
                .addValue("promptTemplateVersion", job.promptTemplateVersion())
                .addValue("errorMessage", job.errorMessage())
                .addValue("createdBy", job.createdBy())
                .addValue("startedAt", job.startedAt() != null ? Timestamp.from(job.startedAt()) : null)
                .addValue("completedAt", job.completedAt() != null ? Timestamp.from(job.completedAt()) : null)
                .addValue("createdAt", Timestamp.from(job.createdAt())));
        return job;
    }

    @Override
    public Optional<PracticeGenerationJob> findJobById(UUID id) {
        return jdbc.query("SELECT * FROM practice_generation_jobs WHERE id = :id",
                new MapSqlParameterSource("id", id), JOB_MAPPER).stream().findFirst();
    }

    @Override
    public List<PracticeGenerationJob> listJobs(int limit, int offset) {
        return jdbc.query("SELECT * FROM practice_generation_jobs ORDER BY created_at DESC LIMIT :limit OFFSET :offset",
                new MapSqlParameterSource().addValue("limit", Math.max(1, limit)).addValue("offset", Math.max(0, offset)), JOB_MAPPER);
    }

    @Override
    public void updateJobStatus(UUID jobId, String status, String errorMessage) {
        Instant now = Instant.now();
        Timestamp completedAt = "COMPLETED".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status) ? Timestamp.from(now) : null;
        jdbc.update("""
                UPDATE practice_generation_jobs
                SET status = :status, error_message = :errorMessage, completed_at = COALESCE(:completedAt, completed_at)
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", jobId)
                .addValue("status", status)
                .addValue("errorMessage", errorMessage)
                .addValue("completedAt", completedAt));
    }

    // Sets & Versions
    @Override
    public GeneratedPracticeSet saveSet(GeneratedPracticeSet set) {
        jdbc.update("""
                INSERT INTO generated_practice_sets
                (id, job_id, skill, title, current_version_id, state, published_set_id, approved_by, approved_at, created_at, updated_at)
                VALUES (:id, :jobId, :skill, :title, :currentVersionId, :state, :publishedSetId, :approvedBy, :approvedAt, :createdAt, :updatedAt)
                ON CONFLICT (id) DO UPDATE SET
                    title = EXCLUDED.title,
                    current_version_id = EXCLUDED.current_version_id,
                    state = EXCLUDED.state,
                    published_set_id = EXCLUDED.published_set_id,
                    approved_by = EXCLUDED.approved_by,
                    approved_at = EXCLUDED.approved_at,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", set.id())
                .addValue("jobId", set.jobId())
                .addValue("skill", set.skill().name())
                .addValue("title", set.title())
                .addValue("currentVersionId", set.currentVersionId())
                .addValue("state", set.state().name())
                .addValue("publishedSetId", set.publishedSetId())
                .addValue("approvedBy", set.approvedBy())
                .addValue("approvedAt", set.approvedAt() != null ? Timestamp.from(set.approvedAt()) : null)
                .addValue("createdAt", Timestamp.from(set.createdAt()))
                .addValue("updatedAt", Timestamp.from(set.updatedAt())));
        return set;
    }

    @Override
    public Optional<GeneratedPracticeSet> findSetById(UUID id) {
        return jdbc.query("SELECT * FROM generated_practice_sets WHERE id = :id",
                new MapSqlParameterSource("id", id), SET_MAPPER).stream().findFirst();
    }

    @Override
    public List<GeneratedPracticeSet> listSets(GenerationState filterState, int limit, int offset) {
        if (filterState != null) {
            return jdbc.query("SELECT * FROM generated_practice_sets WHERE state = :state ORDER BY created_at DESC LIMIT :limit OFFSET :offset",
                    new MapSqlParameterSource().addValue("state", filterState.name()).addValue("limit", Math.max(1, limit)).addValue("offset", Math.max(0, offset)), SET_MAPPER);
        }
        return jdbc.query("SELECT * FROM generated_practice_sets ORDER BY created_at DESC LIMIT :limit OFFSET :offset",
                new MapSqlParameterSource().addValue("limit", Math.max(1, limit)).addValue("offset", Math.max(0, offset)), SET_MAPPER);
    }

    @Override
    public void updateSetState(UUID setId, GenerationState state, UUID currentVersionId) {
        jdbc.update("""
                UPDATE generated_practice_sets
                SET state = :state, current_version_id = COALESCE(:currentVersionId, current_version_id), updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", setId)
                .addValue("state", state.name())
                .addValue("currentVersionId", currentVersionId));
    }

    @Override
    public void markSetApproved(UUID setId, String publishedSetId, UUID approvedBy) {
        jdbc.update("""
                UPDATE generated_practice_sets
                SET state = 'APPROVED', published_set_id = :publishedSetId, approved_by = :approvedBy, approved_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", setId)
                .addValue("publishedSetId", publishedSetId)
                .addValue("approvedBy", approvedBy));
    }

    @Override
    public GeneratedPracticeVersion saveVersion(GeneratedPracticeVersion version) {
        jdbc.update("""
                INSERT INTO generated_practice_versions
                (id, set_id, version_number, passage_content, questions_payload, novelty_report, created_at)
                VALUES (:id, :setId, :versionNumber, :passageContent::jsonb, :questionsPayload::jsonb, :noveltyReport::jsonb, :createdAt)
                ON CONFLICT (id) DO UPDATE SET
                    passage_content = EXCLUDED.passage_content,
                    questions_payload = EXCLUDED.questions_payload,
                    novelty_report = EXCLUDED.novelty_report
                """, new MapSqlParameterSource()
                .addValue("id", version.id())
                .addValue("setId", version.setId())
                .addValue("versionNumber", version.versionNumber())
                .addValue("passageContent", version.passageContent())
                .addValue("questionsPayload", version.questionsPayload())
                .addValue("noveltyReport", version.noveltyReport())
                .addValue("createdAt", Timestamp.from(version.createdAt())));
        return version;
    }

    @Override
    public Optional<GeneratedPracticeVersion> findVersionById(UUID id) {
        return jdbc.query("SELECT * FROM generated_practice_versions WHERE id = :id",
                new MapSqlParameterSource("id", id), VERSION_MAPPER).stream().findFirst();
    }

    @Override
    public Optional<GeneratedPracticeVersion> findVersionBySetAndNumber(UUID setId, int versionNumber) {
        return jdbc.query("SELECT * FROM generated_practice_versions WHERE set_id = :setId AND version_number = :versionNumber",
                new MapSqlParameterSource().addValue("setId", setId).addValue("versionNumber", versionNumber), VERSION_MAPPER).stream().findFirst();
    }

    @Override
    public List<GeneratedPracticeVersion> listVersionsForSet(UUID setId) {
        return jdbc.query("SELECT * FROM generated_practice_versions WHERE set_id = :setId ORDER BY version_number ASC",
                new MapSqlParameterSource("setId", setId), VERSION_MAPPER);
    }

    // Validation Results
    @Override
    public GenerationValidationResult saveValidationResult(GenerationValidationResult result) {
        jdbc.update("""
                INSERT INTO generation_validation_results
                (id, version_id, validator_name, status, findings, executed_at)
                VALUES (:id, :versionId, :validatorName, :status, :findings::jsonb, :executedAt)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    findings = EXCLUDED.findings,
                    executed_at = EXCLUDED.executed_at
                """, new MapSqlParameterSource()
                .addValue("id", result.id())
                .addValue("versionId", result.versionId())
                .addValue("validatorName", result.validatorName())
                .addValue("status", result.status().name())
                .addValue("findings", result.findings())
                .addValue("executedAt", Timestamp.from(result.executedAt())));
        return result;
    }

    @Override
    public List<GenerationValidationResult> listValidationResultsForVersion(UUID versionId) {
        return jdbc.query("SELECT * FROM generation_validation_results WHERE version_id = :versionId ORDER BY executed_at ASC",
                new MapSqlParameterSource("versionId", versionId), VALIDATION_MAPPER);
    }

    // Review Actions
    @Override
    public PracticeReviewAction saveReviewAction(PracticeReviewAction action) {
        jdbc.update("""
                INSERT INTO practice_review_actions
                (id, set_id, version_id, admin_id, action, reviewer_notes, created_at)
                VALUES (:id, :setId, :versionId, :adminId, :action, :reviewerNotes, :createdAt)
                """, new MapSqlParameterSource()
                .addValue("id", action.id())
                .addValue("setId", action.setId())
                .addValue("versionId", action.versionId())
                .addValue("adminId", action.adminId())
                .addValue("action", action.action())
                .addValue("reviewerNotes", action.reviewerNotes())
                .addValue("createdAt", Timestamp.from(action.createdAt())));
        return action;
    }

    @Override
    public List<PracticeReviewAction> listReviewActionsForSet(UUID setId) {
        return jdbc.query("SELECT * FROM practice_review_actions WHERE set_id = :setId ORDER BY created_at DESC",
                new MapSqlParameterSource("setId", setId), REVIEW_ACTION_MAPPER);
    }

    // Row Mappers
    private static final RowMapper<PracticeGenerationSource> SOURCE_MAPPER = (rs, rowNum) -> new PracticeGenerationSource(
            rs.getObject("id", UUID.class),
            rs.getString("title"),
            rs.getString("source_type"),
            rs.getString("author"),
            RightsStatus.valueOf(rs.getString("rights_status")),
            rs.getString("license_note"),
            rs.getString("normalized_content"),
            rs.getString("checksum"),
            rs.getObject("created_by", UUID.class),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")));

    private static final RowMapper<PracticeGenerationBlueprint> BLUEPRINT_MAPPER = (rs, rowNum) -> new PracticeGenerationBlueprint(
            rs.getObject("id", UUID.class),
            Skill.valueOf(rs.getString("skill")),
            rs.getString("title"),
            rs.getBigDecimal("target_band"),
            rs.getString("blueprint_schema"),
            rs.getObject("created_by", UUID.class),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")));

    private static final RowMapper<PracticeGenerationJob> JOB_MAPPER = (rs, rowNum) -> new PracticeGenerationJob(
            rs.getObject("id", UUID.class),
            rs.getObject("source_id", UUID.class),
            rs.getObject("blueprint_id", UUID.class),
            Skill.valueOf(rs.getString("skill")),
            rs.getString("status"),
            rs.getString("model_id"),
            rs.getString("prompt_template_version"),
            rs.getString("error_message"),
            rs.getObject("created_by", UUID.class),
            toInstant(rs.getTimestamp("started_at")),
            toInstant(rs.getTimestamp("completed_at")),
            toInstant(rs.getTimestamp("created_at")));

    private static final RowMapper<GeneratedPracticeSet> SET_MAPPER = (rs, rowNum) -> new GeneratedPracticeSet(
            rs.getObject("id", UUID.class),
            rs.getObject("job_id", UUID.class),
            Skill.valueOf(rs.getString("skill")),
            rs.getString("title"),
            rs.getObject("current_version_id", UUID.class),
            GenerationState.valueOf(rs.getString("state")),
            rs.getString("published_set_id"),
            rs.getObject("approved_by", UUID.class),
            toInstant(rs.getTimestamp("approved_at")),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")));

    private static final RowMapper<GeneratedPracticeVersion> VERSION_MAPPER = (rs, rowNum) -> new GeneratedPracticeVersion(
            rs.getObject("id", UUID.class),
            rs.getObject("set_id", UUID.class),
            rs.getInt("version_number"),
            rs.getString("passage_content"),
            rs.getString("questions_payload"),
            rs.getString("novelty_report"),
            toInstant(rs.getTimestamp("created_at")));

    private static final RowMapper<GenerationValidationResult> VALIDATION_MAPPER = (rs, rowNum) -> new GenerationValidationResult(
            rs.getObject("id", UUID.class),
            rs.getObject("version_id", UUID.class),
            rs.getString("validator_name"),
            ValidationStatus.valueOf(rs.getString("status")),
            rs.getString("findings"),
            toInstant(rs.getTimestamp("executed_at")));

    private static final RowMapper<PracticeReviewAction> REVIEW_ACTION_MAPPER = (rs, rowNum) -> new PracticeReviewAction(
            rs.getObject("id", UUID.class),
            rs.getObject("set_id", UUID.class),
            rs.getObject("version_id", UUID.class),
            rs.getObject("admin_id", UUID.class),
            rs.getString("action"),
            rs.getString("reviewer_notes"),
            toInstant(rs.getTimestamp("created_at")));

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
