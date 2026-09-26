package com.ieltsaitutor.tutor.memory;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltsaitutor.ai.dto.AiSource;

@Repository
public class JdbcConversationRepository implements ConversationRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();
    public JdbcConversationRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void saveConversation(AiConversation conversation) {
        jdbc.update("INSERT INTO ai_conversations(id,user_id,skill,practice_set_id,attempt_id,question_id,title,status,created_at,updated_at) "
                + "VALUES(:id,:userId,:skill,:setId,:attemptId,:questionId,:title,:status,:createdAt,:updatedAt)",
                new MapSqlParameterSource().addValue("id", conversation.id()).addValue("userId", conversation.userId())
                        .addValue("skill", conversation.skill()).addValue("setId", conversation.practiceSetId())
                        .addValue("attemptId", conversation.attemptId()).addValue("questionId", conversation.questionId())
                        .addValue("title", conversation.title()).addValue("status", conversation.status().name())
                        .addValue("createdAt", Timestamp.from(conversation.createdAt())).addValue("updatedAt", Timestamp.from(conversation.updatedAt())));
    }

    @Override public Optional<AiConversation> findConversation(UUID userId, UUID id) {
        return jdbc.query("SELECT * FROM ai_conversations WHERE user_id=:userId AND id=:id",
                new MapSqlParameterSource().addValue("userId", userId).addValue("id", id), (rs, row) -> new AiConversation(
                        (UUID) rs.getObject("id"), (UUID) rs.getObject("user_id"), rs.getString("skill"), rs.getString("practice_set_id"),
                        (UUID) rs.getObject("attempt_id"), rs.getString("question_id"), rs.getString("title"),
                        ConversationStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()))
                .stream().findFirst();
    }

    @Override public List<AiConversation> findConversations(UUID userId) {
        return jdbc.query("SELECT * FROM ai_conversations WHERE user_id=:userId ORDER BY updated_at DESC",
                new MapSqlParameterSource("userId", userId), (rs, row) -> new AiConversation(
                        (UUID) rs.getObject("id"), (UUID) rs.getObject("user_id"), rs.getString("skill"), rs.getString("practice_set_id"),
                        (UUID) rs.getObject("attempt_id"), rs.getString("question_id"), rs.getString("title"),
                        ConversationStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()));
    }

    @Override public List<AiMessage> findMessages(UUID userId, UUID id) {
        return jdbc.query("SELECT m.* FROM ai_messages m JOIN ai_conversations c ON c.id=m.conversation_id WHERE c.user_id=:userId AND c.id=:id ORDER BY m.sequence_no",
                new MapSqlParameterSource().addValue("userId", userId).addValue("id", id), (rs, row) -> new AiMessage(
                        (UUID) rs.getObject("id"), (UUID) rs.getObject("conversation_id"), rs.getInt("sequence_no"),
                        AiMessageRole.valueOf(rs.getString("role")), rs.getString("content"), rs.getString("response_status"),
                        rs.getString("grounding_status"), List.of(), java.util.Map.of(), rs.getTimestamp("created_at").toInstant()));
    }

    @Override public boolean archive(UUID userId, UUID id) {
        return jdbc.update("UPDATE ai_conversations SET status='ARCHIVED', updated_at=CURRENT_TIMESTAMP WHERE user_id=:userId AND id=:id",
                new MapSqlParameterSource().addValue("userId", userId).addValue("id", id)) > 0;
    }

    @Override public void saveMessage(AiMessage message) {
        try {
            jdbc.update("INSERT INTO ai_messages(id,conversation_id,sequence_no,role,content,response_status,grounding_status,citations_json,context_snapshot_json,created_at) "
                    + "VALUES(:id,:conversationId,:sequenceNo,:role,:content,:responseStatus,:groundingStatus,CAST(:citations AS jsonb),CAST(:context AS jsonb),:createdAt)",
                    new MapSqlParameterSource().addValue("id", message.id()).addValue("conversationId", message.conversationId())
                            .addValue("sequenceNo", message.sequenceNo()).addValue("role", message.role().name()).addValue("content", message.content())
                            .addValue("responseStatus", message.responseStatus()).addValue("groundingStatus", message.groundingStatus())
                            .addValue("citations", mapper.writeValueAsString(message.citations())).addValue("context", mapper.writeValueAsString(message.contextSnapshot()))
                            .addValue("createdAt", Timestamp.from(message.createdAt())));
        } catch (JsonProcessingException e) { throw new IllegalArgumentException("conversation message cannot be serialized", e); }
    }
}
