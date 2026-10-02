package com.ieltsaitutor.diagnostic;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltsaitutor.learning.intelligence.Skill;

@Repository
public class JdbcDiagnosticSessionRepository implements DiagnosticSessionRepository {
    private static final String COLUMNS = "id,user_id,state,definition_version,attempt_number,content_snapshot,"
            + "started_at,submitted_at,version,created_at,updated_at";

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RowMapper<DiagnosticSession> rowMapper = (rs, row) -> new DiagnosticSession(
            rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
            DiagnosticState.valueOf(rs.getString("state")), rs.getString("definition_version"),
            rs.getInt("attempt_number"), parseSections(rs.getString("content_snapshot")),
            rs.getTimestamp("started_at").toInstant(), instant(rs.getTimestamp("submitted_at")),
            rs.getLong("version"), rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());

    public JdbcDiagnosticSessionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public DiagnosticSession create(DiagnosticSession session) {
        jdbc.update("""
                INSERT INTO diagnostic_sessions(id,user_id,state,definition_version,attempt_number,
                    content_snapshot,started_at,submitted_at,version,created_at,updated_at)
                VALUES(:id,:userId,:state,:definitionVersion,:attemptNumber,CAST(:snapshot AS jsonb),
                    :startedAt,:submittedAt,:version,:createdAt,:updatedAt)
                """, new MapSqlParameterSource().addValue("id", session.id())
                .addValue("userId", session.userId())
                .addValue("state", session.state().name())
                .addValue("definitionVersion", session.definitionVersion())
                .addValue("attemptNumber", session.attemptNumber())
                .addValue("snapshot", snapshot(session.sections()))
                .addValue("startedAt", Timestamp.from(session.startedAt()))
                .addValue("submittedAt", timestamp(session.submittedAt()))
                .addValue("version", session.version())
                .addValue("createdAt", Timestamp.from(session.createdAt()))
                .addValue("updatedAt", Timestamp.from(session.updatedAt())));
        return session;
    }

    @Override
    public Optional<DiagnosticSession> find(UUID userId, UUID sessionId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM diagnostic_sessions WHERE user_id=:userId AND id=:id",
                new MapSqlParameterSource().addValue("userId", userId).addValue("id", sessionId), rowMapper)
                .stream().findFirst();
    }

    @Override
    public Optional<DiagnosticSession> findActive(UUID userId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM diagnostic_sessions WHERE user_id=:userId"
                        + " AND state='IN_PROGRESS' ORDER BY started_at DESC LIMIT 1",
                new MapSqlParameterSource("userId", userId), rowMapper).stream().findFirst();
    }

    @Override
    public boolean update(DiagnosticSession session, long expectedVersion) {
        return jdbc.update("""
                UPDATE diagnostic_sessions SET state=:state, content_snapshot=CAST(:snapshot AS jsonb),
                    submitted_at=:submittedAt, version=:version, updated_at=:updatedAt
                WHERE id=:id AND user_id=:userId AND version=:expectedVersion
                """, new MapSqlParameterSource().addValue("state", session.state().name())
                .addValue("snapshot", snapshot(session.sections()))
                .addValue("submittedAt", timestamp(session.submittedAt()))
                .addValue("version", session.version())
                .addValue("updatedAt", Timestamp.from(session.updatedAt()))
                .addValue("id", session.id())
                .addValue("userId", session.userId())
                .addValue("expectedVersion", expectedVersion)) == 1;
    }

    @Override
    public List<DiagnosticSession> history(UUID userId, int limit) {
        return jdbc.query("SELECT " + COLUMNS + " FROM diagnostic_sessions WHERE user_id=:userId"
                + " ORDER BY attempt_number DESC LIMIT :limit",
                new MapSqlParameterSource().addValue("userId", userId).addValue("limit", limit), rowMapper);
    }

    @Override
    public int nextAttemptNumber(UUID userId) {
        Integer next = jdbc.queryForObject("SELECT COALESCE(MAX(attempt_number),0)+1 FROM diagnostic_sessions"
                + " WHERE user_id=:userId", new MapSqlParameterSource("userId", userId), Integer.class);
        return next == null ? 1 : next;
    }

    private String snapshot(List<DiagnosticSectionPin> sections) {
        try {
            List<SectionSnapshot> payload = new ArrayList<>();
            for (DiagnosticSectionPin section : sections) {
                payload.add(new SectionSnapshot(section.skill().name(), section.availability().name(),
                        section.publishedSetId(), section.practiceVersionId(), section.publicationRevision(),
                        section.provenanceReference(), section.reason(),
                        section.submissionId() == null ? null : section.submissionId().toString(),
                        section.started()));
            }
            return mapper.writeValueAsString(payload);
        } catch (Exception error) {
            throw new IllegalArgumentException("Invalid diagnostic content snapshot", error);
        }
    }

    private List<DiagnosticSectionPin> parseSections(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return List.of();
        }
        try {
            List<SectionSnapshot> payload = mapper.readValue(snapshot, new TypeReference<>() { });
            List<DiagnosticSectionPin> sections = new ArrayList<>();
            for (SectionSnapshot entry : payload) {
                sections.add(new DiagnosticSectionPin(Skill.valueOf(entry.skill()),
                        DiagnosticAvailability.valueOf(entry.availability()), entry.publishedSetId(),
                        entry.practiceVersionId(), entry.publicationRevision(), entry.provenanceReference(),
                        entry.reason(), entry.submissionId() == null ? null : UUID.fromString(entry.submissionId()),
                        entry.started()));
            }
            return sections;
        } catch (Exception error) {
            throw new IllegalArgumentException("Invalid diagnostic content snapshot", error);
        }
    }

    private static Timestamp timestamp(java.time.Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static java.time.Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    /** Serialized form of a pinned section; kept flat so the snapshot is auditable. */
    private record SectionSnapshot(String skill, String availability, String publishedSetId,
            String practiceVersionId, int publicationRevision, String provenanceReference, String reason,
            String submissionId, boolean started) {
    }
}