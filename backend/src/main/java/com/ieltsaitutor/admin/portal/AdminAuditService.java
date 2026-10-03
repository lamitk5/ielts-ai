package com.ieltsaitutor.admin.portal;

import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditService {
    private final NamedParameterJdbcTemplate jdbc;

    public AdminAuditService(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void record(UUID actor, String action, String objectType, String objectId, String metadata) {
        jdbc.update("INSERT INTO audit_events(id,actor_user_id,action,object_type,object_id,metadata,created_at) "
                + "VALUES(:id,:actor,:action,:objectType,:objectId,CAST(:metadata AS jsonb),:createdAt)",
                new MapSqlParameterSource().addValue("id", UUID.randomUUID()).addValue("actor", actor)
                        .addValue("action", action).addValue("objectType", objectType).addValue("objectId", objectId)
                        .addValue("metadata", metadata == null || metadata.isBlank() ? "{}" : metadata)
                        .addValue("createdAt", Instant.now()));
    }
}
