package com.ieltsaitutor.tutor.tool;

public record TutorToolResult(String status, String answer) {
    public static TutorToolResult missing() { return new TutorToolResult("CONTEXT_MISSING", "Chưa có đủ ngữ cảnh học tập để trả lời chắc chắn."); }
    public static TutorToolResult appData(String answer) { return new TutorToolResult("APP_DATA", answer); }
}
