package com.ieltsaitutor.preferences;

import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserPreferencesRepository implements UserPreferencesRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcUserPreferencesRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public UserPreferences find(UUID userId) {
        return jdbc.query("SELECT * FROM user_preferences WHERE user_id=:userId",
                new MapSqlParameterSource("userId", userId), (rs, row) -> new UserPreferences(
                        rs.getString("theme_mode"), rs.getString("accent_preset"), rs.getString("font_scale"),
                        rs.getString("density"), rs.getString("reduce_motion"), rs.getBoolean("proactive_ai_enabled"),
                        rs.getBoolean("cross_highlight_enabled"), rs.getBoolean("timer_default_enabled"),
                        rs.getInt("reading_split_ratio"), rs.getInt("writing_split_ratio"), rs.getString("language"), rs.getLong("version"),
                        rs.getTimestamp("updated_at").toInstant())).stream().findFirst().orElse(null);
    }

    @Override
    public void insertDefault(UUID userId) {
        jdbc.update("INSERT INTO user_preferences(user_id) VALUES(:userId) ON CONFLICT (user_id) DO NOTHING",
                new MapSqlParameterSource("userId", userId));
    }

    @Override
    public boolean update(UUID userId, UserPreferences value, long expectedVersion) {
        return jdbc.update("""
                UPDATE user_preferences SET theme_mode=:themeMode, accent_preset=:accentPreset,
                    font_scale=:fontScale, density=:density, reduce_motion=:reduceMotion,
                    proactive_ai_enabled=:proactiveAiEnabled, cross_highlight_enabled=:crossHighlightEnabled,
                    timer_default_enabled=:timerDefaultEnabled, reading_split_ratio=:readingSplitRatio,
                    writing_split_ratio=:writingSplitRatio, language=:language, version=version+1, updated_at=CURRENT_TIMESTAMP
                WHERE user_id=:userId AND version=:expectedVersion
                """, new MapSqlParameterSource().addValue("userId", userId)
                .addValue("themeMode", value.themeMode()).addValue("accentPreset", value.accentPreset())
                .addValue("fontScale", value.fontScale()).addValue("density", value.density())
                .addValue("reduceMotion", value.reduceMotion())
                .addValue("proactiveAiEnabled", value.proactiveAiEnabled())
                .addValue("crossHighlightEnabled", value.crossHighlightEnabled())
                .addValue("timerDefaultEnabled", value.timerDefaultEnabled())
                .addValue("readingSplitRatio", value.readingSplitRatio())
                .addValue("writingSplitRatio", value.writingSplitRatio())
                .addValue("language", value.language())
                .addValue("expectedVersion", expectedVersion)) == 1;
    }
}
