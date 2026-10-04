package com.ieltsaitutor.learning.notebook;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcErrorNotebookAcknowledgementRepository implements ErrorNotebookAcknowledgementRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcErrorNotebookAcknowledgementRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Optional<String> findState(UUID userId, UUID mistakeId) {
        return jdbc.query("SELECT state FROM learning_mistake_acknowledgements WHERE user_id=:userId AND mistake_id=:mistakeId",
                new MapSqlParameterSource().addValue("userId", userId).addValue("mistakeId", mistakeId), (rs, row) -> rs.getString("state")).stream().findFirst();
    }
    public void acknowledge(UUID userId, UUID mistakeId) {
        jdbc.update("INSERT INTO learning_mistake_acknowledgements(id,user_id,mistake_id,state,created_at,updated_at) VALUES(:id,:userId,:mistakeId,'ACKNOWLEDGED',:now,:now) ON CONFLICT(user_id,mistake_id) DO UPDATE SET state='ACKNOWLEDGED',updated_at=:now",
                new MapSqlParameterSource().addValue("id", UUID.randomUUID()).addValue("userId", userId).addValue("mistakeId", mistakeId).addValue("now", java.sql.Timestamp.from(Instant.now())));
    }
}
