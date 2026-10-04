package com.ieltsaitutor.admin.portal;

import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ApiUsageService {
    private final NamedParameterJdbcTemplate jdbc;
    public ApiUsageService(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void record(String provider, String model, String feature, String status, long latencyMs, boolean fallback) {
        jdbc.update("INSERT INTO api_usage_events(id,provider,model,feature,status,latency_ms,fallback,created_at) VALUES(:id,:provider,:model,:feature,:status,:latency,:fallback,:createdAt)",
                new MapSqlParameterSource().addValue("id", UUID.randomUUID()).addValue("provider", provider)
                        .addValue("model", model).addValue("feature", feature).addValue("status", status)
                        .addValue("latency", latencyMs).addValue("fallback", fallback).addValue("createdAt", Instant.now()));
    }
}
