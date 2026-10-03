package com.ieltsaitutor.admin.portal;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.auth.AuthPrincipal;

@Service
public class AdminPortalService {
    private final NamedParameterJdbcTemplate jdbc;
    private final AdminAuditService audit;

    public AdminPortalService(NamedParameterJdbcTemplate jdbc, AdminAuditService audit) { this.jdbc = jdbc; this.audit = audit; }

    public Map<String, Object> overview(AuthPrincipal principal) {
        AdminAccess.requireAdmin(principal);
        return Map.of("learners", count("SELECT COUNT(*) FROM app_users WHERE role = 'CUSTOMER'"),
                "practiceSets", count("SELECT COUNT(*) FROM generated_practice_sets"),
                "pendingReview", count("SELECT COUNT(*) FROM generated_practice_sets WHERE state IN ('PENDING_REVIEW','NEEDS_REVISION')"),
                "openReports", count("SELECT COUNT(*) FROM submission_reports WHERE status = 'OPEN'"),
                "activePrompts", count("SELECT COUNT(*) FROM prompt_versions WHERE status = 'ACTIVE'"));
    }

    public Page learners(AuthPrincipal principal, String query, int page, int size) {
        AdminAccess.requireAdmin(principal);
        int safeSize = Math.min(Math.max(size, 1), 100); int safePage = Math.max(page, 0);
        String text = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        var params = new MapSqlParameterSource().addValue("query", text).addValue("limit", safeSize).addValue("offset", safePage * safeSize);
        List<Map<String,Object>> items = jdbc.queryForList("SELECT id,email_normalized AS email,first_name AS firstName,role,created_at AS createdAt "
                + "FROM app_users WHERE role = 'CUSTOMER' AND (:query = '' OR lower(email_normalized) LIKE '%' || :query || '%' OR lower(first_name) LIKE '%' || :query || '%') "
                + "ORDER BY created_at DESC LIMIT :limit OFFSET :offset", params);
        long total = count("SELECT COUNT(*) FROM app_users WHERE role = 'CUSTOMER' AND ('" + escape(text) + "' = '' OR lower(email_normalized) LIKE '%" + escape(text) + "%' OR lower(first_name) LIKE '%" + escape(text) + "%')");
        return new Page(items, safePage, safeSize, total);
    }

    public List<Map<String,Object>> practices(AuthPrincipal principal, String state, String skill) {
        AdminAccess.requireAdmin(principal);
        return jdbc.queryForList("SELECT id,skill,title,state,published_set_id AS publishedSetId,updated_at AS updatedAt "
                + "FROM generated_practice_sets WHERE (:state = '' OR state = :state) AND (:skill = '' OR skill = :skill) ORDER BY updated_at DESC",
                new MapSqlParameterSource().addValue("state", state == null ? "" : state.toUpperCase()).addValue("skill", skill == null ? "" : skill.toUpperCase()));
    }

    public List<Map<String,Object>> reports(AuthPrincipal principal) {
        AdminAccess.requireAdmin(principal);
        return jdbc.queryForList("SELECT r.id,r.submission_id AS submissionId,r.owner_user_id AS ownerUserId,r.category,r.comment,r.status,r.created_at AS createdAt,"
                + "u.email_normalized AS ownerEmail,s.skill,s.status AS submissionStatus FROM submission_reports r "
                + "JOIN app_users u ON u.id=r.owner_user_id JOIN practice_submissions s ON s.id=r.submission_id ORDER BY r.created_at DESC", new MapSqlParameterSource());
    }

    public void resolveReport(AuthPrincipal principal, UUID reportId) {
        AdminAccess.requireAdmin(principal);
        int updated = jdbc.update("UPDATE submission_reports SET status='RESOLVED',resolved_at=:now,resolved_by=:admin WHERE id=:id AND status='OPEN'",
                new MapSqlParameterSource().addValue("now", Instant.now()).addValue("admin", principal.userId()).addValue("id", reportId));
        if (updated == 0) throw new IllegalArgumentException("Báo cáo không còn mở.");
        audit.record(principal.userId(), "SUBMISSION_REPORT_RESOLVED", "submission_report", reportId.toString(), "{}");
    }

    public List<Map<String,Object>> usage(AuthPrincipal principal, String provider, String feature) {
        AdminAccess.requireAdmin(principal);
        return jdbc.queryForList("SELECT COALESCE(provider,'UNKNOWN') AS provider,COALESCE(feature,'UNKNOWN') AS feature,COUNT(*) AS requests,"
                + "COALESCE(SUM(total_tokens),0) AS totalTokens,COALESCE(SUM(estimated_cost),0) AS estimatedCost "
                + "FROM api_usage_events WHERE (:provider='' OR provider=:provider) AND (:feature='' OR feature=:feature) GROUP BY provider,feature ORDER BY requests DESC",
                new MapSqlParameterSource().addValue("provider", provider == null ? "" : provider).addValue("feature", feature == null ? "" : feature));
    }

    public Page audit(AuthPrincipal principal, int page, int size) {
        AdminAccess.requireAdmin(principal);
        int safeSize = Math.min(Math.max(size, 1), 100); int safePage = Math.max(page, 0);
        var params = new MapSqlParameterSource().addValue("limit", safeSize).addValue("offset", safePage * safeSize);
        List<Map<String,Object>> items = jdbc.queryForList("SELECT id,actor_user_id AS actorUserId,action,object_type AS objectType,object_id AS objectId,metadata,created_at AS createdAt FROM audit_events ORDER BY created_at DESC LIMIT :limit OFFSET :offset", params);
        return new Page(items, safePage, safeSize, count("SELECT COUNT(*) FROM audit_events"));
    }

    public List<Map<String,Object>> prompts(AuthPrincipal principal) {
        AdminAccess.requireAdmin(principal);
        return jdbc.queryForList("SELECT d.prompt_key AS promptKey,d.name,d.purpose,v.id AS versionId,v.version_number AS versionNumber,v.status,v.content,v.created_at AS createdAt FROM prompt_definitions d LEFT JOIN prompt_versions v ON v.definition_id=d.id AND v.status IN ('ACTIVE','DRAFT') ORDER BY d.prompt_key,v.version_number DESC", new MapSqlParameterSource());
    }

    public Map<String,Object> createDraft(AuthPrincipal principal, String key, String name, String purpose, String content) {
        AdminAccess.requireAdmin(principal);
        UUID definitionId = jdbc.query("SELECT id FROM prompt_definitions WHERE prompt_key=:key", new MapSqlParameterSource("key", key), (rs, row) -> rs.getObject("id", UUID.class)).stream().findFirst().orElse(null);
        if (definitionId == null) {
            definitionId = UUID.randomUUID();
            jdbc.update("INSERT INTO prompt_definitions(id,prompt_key,name,purpose,created_by,created_at) VALUES(:id,:key,:name,:purpose,:actor,:now)",
                    new MapSqlParameterSource().addValue("id", definitionId).addValue("key", key).addValue("name", name).addValue("purpose", purpose == null ? "" : purpose).addValue("actor", principal.userId()).addValue("now", Instant.now()));
        }
        Integer next = jdbc.queryForObject("SELECT COALESCE(MAX(version_number),0)+1 FROM prompt_versions WHERE definition_id=:id", new MapSqlParameterSource("id", definitionId), Integer.class);
        UUID versionId = UUID.randomUUID();
        jdbc.update("INSERT INTO prompt_versions(id,definition_id,version_number,content,status,created_by,created_at) VALUES(:version,:definition,:number,:content,'DRAFT',:actor,:now)", new MapSqlParameterSource().addValue("version",versionId).addValue("definition",definitionId).addValue("number",next).addValue("content",content).addValue("actor",principal.userId()).addValue("now",Instant.now()));
        audit.record(principal.userId(), "PROMPT_DRAFT_CREATED", "prompt", key, "{\"version\":" + next + "}");
        return Map.of("id", versionId, "promptKey", key, "versionNumber", next, "status", "DRAFT");
    }

    public void activatePrompt(AuthPrincipal principal, UUID versionId) {
        AdminAccess.requireAdmin(principal);
        UUID definition = jdbc.queryForObject("SELECT definition_id FROM prompt_versions WHERE id=:id", new MapSqlParameterSource("id", versionId), UUID.class);
        jdbc.update("UPDATE prompt_versions SET status='ARCHIVED' WHERE definition_id=:definition AND status='ACTIVE'", new MapSqlParameterSource("definition", definition));
        int updated = jdbc.update("UPDATE prompt_versions SET status='ACTIVE',activated_by=:actor,activated_at=:now WHERE id=:id AND status='DRAFT'", new MapSqlParameterSource().addValue("id",versionId).addValue("actor",principal.userId()).addValue("now",Instant.now()));
        if (updated == 0) throw new IllegalArgumentException("Prompt version không còn ở trạng thái bản nháp.");
        audit.record(principal.userId(), "PROMPT_ACTIVATED", "prompt_version", versionId.toString(), "{}");
    }

    private long count(String sql) { Long value = jdbc.getJdbcTemplate().queryForObject(sql, Long.class); return value == null ? 0 : value; }
    private String escape(String value) { return value.replace("'", "''"); }
    public record Page(List<Map<String,Object>> items, int page, int size, long total) {}
}
