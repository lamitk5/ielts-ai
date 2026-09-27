package com.ieltsaitutor.practice.catalog;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPracticePublicationRepository implements PracticePublicationRepository {
    private static final RowMapper<PracticePublication> MAPPER = (rs, rowNum) -> new PracticePublication(
            rs.getString("published_set_id"),
            rs.getObject("generated_set_id", UUID.class),
            rs.getObject("generated_version_id", UUID.class),
            rs.getString("skill"),
            rs.getBoolean("active"),
            rs.getInt("publication_revision"),
            rs.getString("provenance_reference"),
            rs.getTimestamp("published_at").toInstant());

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPracticePublicationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PracticePublication save(PracticePublication publication) {
        jdbc.update("""
                INSERT INTO practice_catalog_publications
                    (published_set_id, generated_set_id, generated_version_id, skill, active, publication_revision, provenance_reference, published_at)
                VALUES (:publishedSetId, :generatedSetId, :generatedVersionId, :skill, TRUE, :publicationRevision, :provenanceReference, :publishedAt)
                ON CONFLICT (published_set_id) DO UPDATE SET
                    generated_set_id = EXCLUDED.generated_set_id,
                    generated_version_id = EXCLUDED.generated_version_id,
                    skill = EXCLUDED.skill,
                    active = TRUE,
                    publication_revision = EXCLUDED.publication_revision,
                    provenance_reference = EXCLUDED.provenance_reference,
                    published_at = EXCLUDED.published_at
                """, params(publication));
        return publication;
    }

    @Override
    public List<PracticePublication> findActiveBySkill(String skill) {
        return jdbc.query("SELECT * FROM practice_catalog_publications WHERE active = TRUE AND lower(skill) = lower(:skill) ORDER BY published_at DESC",
                new MapSqlParameterSource("skill", skill), MAPPER);
    }

    @Override
    public Optional<PracticePublication> findActiveById(String publishedSetId) {
        return jdbc.query("SELECT * FROM practice_catalog_publications WHERE active = TRUE AND published_set_id = :publishedSetId",
                new MapSqlParameterSource("publishedSetId", publishedSetId), MAPPER).stream().findFirst();
    }

    @Override
    public void deactivate(String publishedSetId) {
        jdbc.update("UPDATE practice_catalog_publications SET active = FALSE WHERE published_set_id = :publishedSetId",
                new MapSqlParameterSource("publishedSetId", publishedSetId));
    }

    private static MapSqlParameterSource params(PracticePublication publication) {
        return new MapSqlParameterSource()
                .addValue("publishedSetId", publication.publishedSetId())
                .addValue("generatedSetId", publication.generatedSetId())
                .addValue("generatedVersionId", publication.generatedVersionId())
                .addValue("skill", publication.skill().toLowerCase())
                .addValue("publicationRevision", publication.publicationRevision())
                .addValue("provenanceReference", publication.provenanceReference())
                .addValue("publishedAt", Timestamp.from(publication.publishedAt()));
    }
}
