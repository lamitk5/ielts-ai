package com.ieltsaitutor.onboarding;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ieltsaitutor.learning.intelligence.Skill;

@Repository
public class JdbcLearnerOnboardingRepository implements LearnerOnboardingRepository {
    private static final String COLUMNS = "self_reported_level, target_band, target_exam_date, "
            + "perceived_weakest_skill, daily_study_minutes, study_days_per_week, state, source, basis, "
            + "measured_level, measured_weakest_skill, evidence_reference, version, created_at, updated_at";

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcLearnerOnboardingRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public LearnerOnboardingProfile find(UUID userId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM learner_onboarding_profiles WHERE user_id=:userId",
                new MapSqlParameterSource("userId", userId), (rs, row) -> new LearnerOnboardingProfile(
                        userId, rs.getString("self_reported_level"), (Double) rs.getObject("target_band"),
                        date(rs.getObject("target_exam_date")), skill(rs.getString("perceived_weakest_skill")),
                        nullableInt(rs, "daily_study_minutes"), nullableInt(rs, "study_days_per_week"),
                        OnboardingState.valueOf(rs.getString("state")), rs.getString("source"),
                        rs.getString("basis"), rs.getString("measured_level"),
                        skill(rs.getString("measured_weakest_skill")), rs.getString("evidence_reference"),
                        rs.getLong("version"), rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()))
                .stream().findFirst().orElse(null);
    }

    @Override
    public void insertDefault(UUID userId, Instant now) {
        jdbc.update("INSERT INTO learner_onboarding_profiles(user_id, state, source, basis, version,"
                        + " created_at, updated_at) VALUES(:userId, 'NOT_STARTED', 'SELF_REPORTED',"
                        + " 'LEARNER_DECLARATION', 0, :now, :now) ON CONFLICT (user_id) DO NOTHING",
                new MapSqlParameterSource("userId", userId).addValue("now", Timestamp.from(now)));
    }

    @Override
    public boolean update(UUID userId, LearnerOnboardingProfile profile, long expectedVersion) {
        return jdbc.update("""
                UPDATE learner_onboarding_profiles SET self_reported_level=:selfReportedLevel,
                    target_band=:targetBand, target_exam_date=:targetExamDate,
                    perceived_weakest_skill=:perceivedWeakestSkill, daily_study_minutes=:dailyStudyMinutes,
                    study_days_per_week=:studyDaysPerWeek, state=:state, source=:source, basis=:basis,
                    measured_level=:measuredLevel, measured_weakest_skill=:measuredWeakestSkill,
                    evidence_reference=:evidenceReference, version=version+1, updated_at=:updatedAt
                WHERE user_id=:userId AND version=:expectedVersion
                """, parameters(userId, profile).addValue("expectedVersion", expectedVersion)) == 1;
    }

    private MapSqlParameterSource parameters(UUID userId, LearnerOnboardingProfile profile) {
        return new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("selfReportedLevel", profile.selfReportedLevel())
                .addValue("targetBand", profile.targetBand())
                .addValue("targetExamDate", profile.targetExamDate() == null
                        ? null : java.sql.Date.valueOf(profile.targetExamDate()))
                .addValue("perceivedWeakestSkill", profile.perceivedWeakestSkill() == null
                        ? null : profile.perceivedWeakestSkill().name())
                .addValue("dailyStudyMinutes", profile.dailyStudyMinutes())
                .addValue("studyDaysPerWeek", profile.studyDaysPerWeek())
                .addValue("state", profile.state().name())
                .addValue("source", profile.source())
                .addValue("basis", profile.basis())
                .addValue("measuredLevel", profile.measuredLevel())
                .addValue("measuredWeakestSkill", profile.measuredWeakestSkill() == null
                        ? null : profile.measuredWeakestSkill().name())
                .addValue("evidenceReference", profile.evidenceReference())
                .addValue("updatedAt", Timestamp.from(profile.updatedAt()));
    }

    private static Integer nullableInt(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static Skill skill(String value) { return value == null ? null : Skill.valueOf(value); }

    private static LocalDate date(Object value) {
        return value == null ? null : ((java.sql.Date) value).toLocalDate();
    }

    static List<String> columns() { return List.of(COLUMNS.split(", ")); }
}