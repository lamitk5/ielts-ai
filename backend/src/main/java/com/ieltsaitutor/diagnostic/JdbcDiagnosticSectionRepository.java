package com.ieltsaitutor.diagnostic;

import com.ieltsaitutor.learning.intelligence.Skill;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcDiagnosticSectionRepository implements DiagnosticSectionRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcDiagnosticSectionRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    public DiagnosticSectionResult save(DiagnosticSectionResult r) {
        jdbc.update("INSERT INTO diagnostic_section_results(id,session_id,user_id,skill,state,score,total,estimated_band,confidence,source_submission_id,source_reference,availability_message,created_at) VALUES(:id,:sessionId,:userId,:skill,:state,:score,:total,:band,:confidence,:submission,:reference,:message,:created) ON CONFLICT(session_id,skill) DO UPDATE SET state=EXCLUDED.state,score=EXCLUDED.score,total=EXCLUDED.total,estimated_band=EXCLUDED.estimated_band,confidence=EXCLUDED.confidence,source_submission_id=EXCLUDED.source_submission_id,source_reference=EXCLUDED.source_reference,availability_message=EXCLUDED.availability_message",
                new MapSqlParameterSource().addValue("id",r.id()).addValue("sessionId",r.sessionId()).addValue("userId",r.userId()).addValue("skill",r.skill().name()).addValue("state",r.state().name()).addValue("score",r.score()).addValue("total",r.total()).addValue("band",r.estimatedBand()).addValue("confidence",r.confidence().name()).addValue("submission",r.sourceSubmissionId()).addValue("reference",r.sourceReference()).addValue("message",r.availabilityMessage()).addValue("created",r.createdAt())); return r;
    }
    public Optional<DiagnosticSectionResult> findOwned(UUID userId, UUID sessionId, Skill skill) { return jdbc.query("SELECT * FROM diagnostic_section_results WHERE user_id=:userId AND session_id=:sessionId AND skill=:skill",new MapSqlParameterSource().addValue("userId",userId).addValue("sessionId",sessionId).addValue("skill",skill.name()),(rs,n)->map(rs)).stream().findFirst(); }
    public List<DiagnosticSectionResult> findOwnedBySession(UUID userId, UUID sessionId) { return jdbc.query("SELECT * FROM diagnostic_section_results WHERE user_id=:userId AND session_id=:sessionId ORDER BY skill",new MapSqlParameterSource().addValue("userId",userId).addValue("sessionId",sessionId),(rs,n)->map(rs)); }
    private DiagnosticSectionResult map(java.sql.ResultSet rs) throws java.sql.SQLException {
        Object band = rs.getObject("estimated_band");
        return new DiagnosticSectionResult((UUID) rs.getObject("id"), (UUID) rs.getObject("session_id"),
                (UUID) rs.getObject("user_id"), Skill.valueOf(rs.getString("skill")),
                DiagnosticSectionState.valueOf(rs.getString("state")), (Integer) rs.getObject("score"),
                (Integer) rs.getObject("total"), band == null ? null : ((Number) band).doubleValue(),
                DiagnosticConfidence.valueOf(rs.getString("confidence")), (UUID) rs.getObject("source_submission_id"),
                rs.getString("source_reference"), rs.getString("availability_message"),
                rs.getTimestamp("created_at").toInstant());
    }
}
