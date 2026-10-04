package com.ieltsaitutor.profile;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLearnerProfileRepository implements LearnerProfileRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcLearnerProfileRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void updateDisplayNameAndAvatar(UUID userId, String firstName, String avatarUrl) {
        String sql = """
                UPDATE app_users
                SET first_name = :firstName,
                    avatar_url = :avatarUrl,
                    updated_at = :updatedAt
                WHERE id = :userId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("firstName", firstName)
                .addValue("avatarUrl", avatarUrl)
                .addValue("updatedAt", Timestamp.from(Instant.now()));
        jdbc.update(sql, params);
    }

    @Override
    public void updatePasswordHash(UUID userId, String passwordHash) {
        String sql = """
                UPDATE app_users
                SET password_hash = :passwordHash,
                    updated_at = :updatedAt
                WHERE id = :userId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("passwordHash", passwordHash)
                .addValue("updatedAt", Timestamp.from(Instant.now()));
        jdbc.update(sql, params);
    }

    @Override
    public String getAvatarUrl(UUID userId) {
        try {
            String sql = "SELECT avatar_url FROM app_users WHERE id = :userId";
            return jdbc.queryForObject(sql, new MapSqlParameterSource("userId", userId), String.class);
        } catch (Exception e) {
            return null;
        }
    }
}
