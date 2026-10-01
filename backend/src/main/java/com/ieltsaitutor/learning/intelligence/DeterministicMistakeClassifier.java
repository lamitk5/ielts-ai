package com.ieltsaitutor.learning.intelligence;

import java.util.Locale;

public class DeterministicMistakeClassifier implements MistakeClassifier {
    @Override
    public MistakeClassification classify(MistakeEvidence evidence) {
        if (evidence == null || evidence.skill() == null) return unknown("UNKNOWN_EVIDENCE");
        if (evidence.correct()) return new MistakeClassification("NO_MISTAKE", 1d, MistakeMethod.DETERMINISTIC,
                "ANSWER_CORRECT", "deterministic answer result is correct");
        String type = upper(evidence.questionType());
        String category = switch (evidence.skill()) {
            case READING -> reading(type);
            case LISTENING -> listening(type, evidence.learnerAnswer(), evidence.correctAnswer());
            case WRITING -> writing(type);
            case SPEAKING -> speaking(type);
        };
        if (category == null || category.startsWith("UNKNOWN_")) return unknown(category == null
                ? "UNKNOWN_" + evidence.skill().name() + "_ERROR" : category);
        return new MistakeClassification(category, 1d, MistakeMethod.DETERMINISTIC,
                "QUESTION_TYPE:" + type, safeEvidence(evidence.learnerAnswer()));
    }

    private String reading(String type) {
        return switch (type) {
            case "TRUE_FALSE_NOT_GIVEN", "TRUE_FALSE", "YES_NO_NOT_GIVEN" -> "TRUE_NOT_GIVEN_CONFUSION";
            case "MATCHING_HEADINGS", "MATCHING_HEADING" -> "MATCHING_HEADING_MAIN_IDEA";
            case "SUMMARY_COMPLETION", "SENTENCE_COMPLETION" -> "SENTENCE_COMPLETION_GRAMMAR";
            case "INFERENCE" -> "INFERENCE_ERROR";
            case "VOCABULARY" -> "VOCABULARY_IN_CONTEXT";
            case "DETAIL" -> "DETAIL_MISREAD";
            default -> "UNKNOWN_READING_ERROR";
        };
    }

    private String listening(String type, String selected, String correct) {
        if ((type.contains("FORM") || type.contains("COMPLETION")) && pluralMismatch(selected, correct)) return "PLURAL_SINGULAR";
        return switch (type) {
            case "MULTIPLE_CHOICE", "DISTRACTOR" -> "DISTRACTOR_TRAP";
            case "NUMBER", "DATE" -> "NUMBER_DATE";
            case "SPELLING" -> "SPELLING";
            case "POSITION" -> "LOST_POSITION";
            case "PARAPHRASE" -> "MISSED_PARAPHRASE";
            case "DETAIL" -> "DETAIL_MISHEARD";
            default -> "UNKNOWN_LISTENING_ERROR";
        };
    }

    private String writing(String type) {
        return switch (type) {
            case "TASK", "TASK_RESPONSE" -> "TASK_ACHIEVEMENT_RESPONSE";
            case "COHERENCE", "COHESION" -> "COHERENCE_COHESION";
            case "LEXICAL", "VOCABULARY" -> "LEXICAL_RESOURCE";
            case "GRAMMAR", "ACCURACY" -> "GRAMMATICAL_RANGE_ACCURACY";
            default -> "UNKNOWN_WRITING_ISSUE";
        };
    }

    private String speaking(String type) {
        return switch (type) {
            case "VOCABULARY" -> "VOCABULARY_RANGE";
            case "GRAMMAR", "ACCURACY" -> "GRAMMAR_ACCURACY";
            case "DEVELOPMENT", "ANSWER" -> "ANSWER_DEVELOPMENT";
            case "FLUENCY", "COHERENCE" -> "COHERENCE_FLUENCY_TEXT_PROXY";
            default -> "UNKNOWN_SPEAKING_ISSUE";
        };
    }

    private boolean pluralMismatch(String selected, String correct) {
        if (selected == null || correct == null || selected.isBlank() || correct.isBlank()) return false;
        return selected.toLowerCase(Locale.ROOT).endsWith("s") != correct.toLowerCase(Locale.ROOT).endsWith("s");
    }

    private MistakeClassification unknown(String code) {
        return new MistakeClassification(code, 0d, MistakeMethod.UNKNOWN, code, "insufficient deterministic evidence");
    }

    private String upper(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private String safeEvidence(String value) { return value == null ? "" : value.length() > 160 ? value.substring(0, 160) : value; }
}
