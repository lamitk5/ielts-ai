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

import com.ieltsaitutor.practice.generator.service.PracticeProvenanceRecord;

@Repository
public class JdbcPracticeProvenanceRepository implements PracticeProvenanceRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPracticeProvenanceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PracticeProvenanceRecord save(PracticeProvenanceRecord record) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("setId", record.setId())
                .addValue("generationJobId", record.generationJobId())
                .addValue("blueprintId", record.blueprintId())
                .addValue("approverId", record.approverId())
                .addValue("approvedAt", Timestamp.from(record.approvedAt()))
                .addValue("provenanceDetails", record.provenanceDetails());

        jdbc.update("""
                INSERT INTO practice_provenance_meta
                (set_id, generation_job_id, blueprint_id, approver_id, approved_at, provenance_details)
                VALUES (:setId, :generationJobId, :blueprintId, :approverId, :approvedAt, CAST(:provenanceDetails AS jsonb))
                ON CONFLICT (set_id) DO UPDATE SET
                    generation_job_id = EXCLUDED.generation_job_id,
                    blueprint_id = EXCLUDED.blueprint_id,
                    approver_id = EXCLUDED.approver_id,
                    approved_at = EXCLUDED.approved_at,
                    provenance_details = EXCLUDED.provenance_details
                """, params);

        return record;
    }

    @Override
    public Optional<PracticeProvenanceRecord> findBySetId(String setId) {
        List<PracticeProvenanceRecord> results = jdbc.query(
                "SELECT * FROM practice_provenance_meta WHERE set_id = :setId",
                Map.of("setId", setId),
                new ProvenanceRowMapper()
        );
        return results.stream().findFirst();
    }

    private static class ProvenanceRowMapper implements RowMapper<PracticeProvenanceRecord> {
        @Override
        public PracticeProvenanceRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            String setId = rs.getString("set_id");
            UUID jobId = rs.getObject("generation_job_id", UUID.class);
            UUID bpId = rs.getObject("blueprint_id", UUID.class);
            UUID approverId = rs.getObject("approver_id", UUID.class);
            Instant approvedAt = rs.getTimestamp("approved_at").toInstant();
            String details = rs.getString("provenance_details");
            return new PracticeProvenanceRecord(setId, jobId, bpId, approverId, approvedAt, details);
        }
    }
}
