package com.ieltsaitutor.assessment.objective;

import java.util.Locale;

/** Applies only the answer normalization allowed by an objective question type. */
public final class AnswerNormalizer {
    private AnswerNormalizer() {}

    public static String normalize(String answer, String questionType) {
        String value = answer == null ? "" : answer.trim();
        String type = upper(questionType);
        if (value.isEmpty()) return "";
        if (isCaseInsensitive(type)) return collapseWhitespace(value).toUpperCase(Locale.ROOT);
        if (isCompletion(type)) return collapseWhitespace(value).toLowerCase(Locale.ROOT);
        return value;
    }

    private static boolean isCaseInsensitive(String type) {
        return type.equals("MULTIPLE_CHOICE") || type.equals("TRUE_FALSE_NOT_GIVEN")
                || type.equals("TRUE_FALSE") || type.equals("YES_NO_NOT_GIVEN")
                || type.equals("MATCHING_HEADINGS") || type.equals("MATCHING_HEADING");
    }

    private static boolean isCompletion(String type) {
        return type.contains("COMPLETION") || type.equals("SHORT_ANSWER") || type.equals("NUMBER")
                || type.equals("DATE") || type.equals("FORM_COMPLETION");
    }

    private static String collapseWhitespace(String value) { return value.replaceAll("\\s+", " "); }
    private static String upper(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
}
