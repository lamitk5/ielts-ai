package com.ieltsaitutor.rag.chat;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.ieltsaitutor.ai.model.AiChatCommand;

@Component
public class RagIntentClassifier {
    private static final Pattern MATERIAL_QUESTION = Pattern.compile(
            "(?iu)(rubric|band descriptor|marking criteria|official criteria|tiêu chí|đáp án|chấm điểm|bài mẫu|task response|coherence|lexical resource|grammar range|phát âm|pronunciation)");

    public boolean requiresRetrieval(AiChatCommand command) {
        if (command == null || command.message() == null) return false;
        var context = command.context();
        return MATERIAL_QUESTION.matcher(command.message()).find()
                || context != null && (hasText(context.lessonId()) || hasText(context.exerciseId())
                        || hasText(context.questionId()) || hasText(context.selectedText()));
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
}
