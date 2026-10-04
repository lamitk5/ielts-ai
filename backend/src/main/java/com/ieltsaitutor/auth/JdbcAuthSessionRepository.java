package com.ieltsaitutor.auth;

import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuthSessionRepository implements AuthSessionRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAuthSessionRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public AuthSession save(AuthSession session) {
        jdbc.update("""
                INSERT INTO auth_sessions(id,user_id,token_hash,expires_at,created_at,revoked_at)
                VALUES(:id,:userId,:tokenHash,:expiresAt,:createdAt,:revokedAt)
                """, params(session));
        return session;
    }

    @Override
    public AuthSession findActiveByTokenHash(String tokenHash, Instant now) {
        return jdbc.query("SELECT * FROM auth_sessions WHERE token_hash=:tokenHash AND revoked_at IS NULL AND expires_at>:now",
                new MapSqlParameterSource().addValue("tokenHash", tokenHash).addValue("now", now.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE),
                (rs, row) -> new AuthSession(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                        rs.getString("token_hash"), rs.getTimestamp("expires_at").toInstant(),
                        rs.getTimestamp("created_at").toInstant(), null)).stream().findFirst().orElse(null);
    }

    @Override
    public void revoke(String tokenHash, Instant revokedAt) {
        jdbc.update("UPDATE auth_sessions SET revoked_at=:revokedAt WHERE token_hash=:tokenHash AND revoked_at IS NULL",
                new MapSqlParameterSource().addValue("tokenHash", tokenHash)
                        .addValue("revokedAt", revokedAt.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE));
    }

    private MapSqlParameterSource params(AuthSession session) {
        return new MapSqlParameterSource().addValue("id", session.id()).addValue("userId", session.userId())
                .addValue("tokenHash", session.tokenHash()).addValue("expiresAt", session.expiresAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("createdAt", session.createdAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("revokedAt", session.revokedAt() == null ? null : session.revokedAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);
    }
}
