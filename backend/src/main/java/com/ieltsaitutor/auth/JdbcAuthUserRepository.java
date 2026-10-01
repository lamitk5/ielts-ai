package com.ieltsaitutor.auth;

import java.sql.Types;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuthUserRepository implements AuthUserRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAuthUserRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public AuthUser findByEmail(String email) {
        return jdbc.query("SELECT * FROM app_users WHERE email_normalized=:email", new MapSqlParameterSource("email", email),
                (rs, row) -> new AuthUser(rs.getObject("id", UUID.class), rs.getString("email_normalized"),
                        rs.getString("first_name"), rs.getString("password_hash"),
                        UserRole.valueOf(rs.getString("role")), rs.getTimestamp("created_at").toInstant()))
                .stream().findFirst().orElse(null);
    }

    @Override
    public AuthUser findById(UUID id) {
        return jdbc.query("SELECT * FROM app_users WHERE id=:id", new MapSqlParameterSource("id", id),
                (rs, row) -> new AuthUser(rs.getObject("id", UUID.class), rs.getString("email_normalized"),
                        rs.getString("first_name"), rs.getString("password_hash"),
                        UserRole.valueOf(rs.getString("role")), rs.getTimestamp("created_at").toInstant()))
                .stream().findFirst().orElse(null);
    }

    @Override
    public AuthUser save(AuthUser user) {
        jdbc.update("""
                INSERT INTO app_users(id,email_normalized,first_name,password_hash,role,created_at)
                VALUES(:id,:email,:firstName,:passwordHash,:role,:createdAt)
                """, new MapSqlParameterSource().addValue("id", user.id()).addValue("email", user.email())
                .addValue("firstName", user.firstName()).addValue("passwordHash", user.passwordHash())
                .addValue("role", user.role().name()).addValue("createdAt", user.createdAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE));
        return user;
    }
}
