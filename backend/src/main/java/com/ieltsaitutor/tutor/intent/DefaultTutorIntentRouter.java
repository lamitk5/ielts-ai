package com.ieltsaitutor.tutor.intent;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.tutor.context.TutorLearningContext;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DefaultTutorIntentRouter implements TutorIntentRouter {
    @Override
    public TutorIntentRoute route(AiChatRequest request, TutorLearningContext context) {
        String message = request == null || request.message() == null ? "" : request.message().trim().toLowerCase(Locale.ROOT);
        String skill = request == null || request.context() == null ? "general"
                : request.context().normalizedSkill().toLowerCase(Locale.ROOT);

        if (matches(message, "đang làm câu nào", "what question am i", "đáp án tôi vừa chọn", "what answer did i choose",
                "tôi được bao nhiêu điểm", "what was my score", "đúng bao nhiêu câu", "how many correct")) {
            return new TutorIntentRoute(TutorIntent.APP_DATA, false, false, "trusted application data");
        }
        if (matches(message, "tiến bộ", "progress", "lịch sử", "history", "recent attempts", "kết quả gần đây")) {
            return new TutorIntentRoute(TutorIntent.PROGRESS_HISTORY, false, false, "trusted progress data");
        }
        if (matches(message, "tại sao", "vì sao", "why", "giải thích câu", "explain this question", "gợi ý", "hint", "wrong", "sai")) {
            boolean hasExercise = context != null && context.available() && context.questionId() != null;
            return new TutorIntentRoute(TutorIntent.EXERCISE_EXPLANATION, hasExercise, hasExercise,
                    hasExercise ? "trusted exercise context" : "exercise context missing");
        }
        if ("writing".equals(skill) || matches(message, "writing", "bài viết", "draft", "task 1", "task 2", "essay")) {
            return new TutorIntentRoute(TutorIntent.WRITING_FEEDBACK, true, false, "writing context");
        }
        if ("speaking".equals(skill) || matches(message, "speaking", "nói", "pronunciation", "part 1", "part 2", "part 3")) {
            return new TutorIntentRoute(TutorIntent.SPEAKING_FEEDBACK, true, false, "speaking context");
        }
        if (matches(message, "source", "citation", "trích dẫn", "theo tài liệu", "according to", "rubric", "band descriptors")) {
            return new TutorIntentRoute(TutorIntent.RAG_EXPLANATION, true, true, "governed source request");
        }
        return new TutorIntentRoute(TutorIntent.GENERIC_CHAT, true, false, "normal Tutor chat");
    }

    private boolean matches(String message, String... terms) {
        for (String term : terms) if (message.contains(term)) return true;
        return false;
    }
}
