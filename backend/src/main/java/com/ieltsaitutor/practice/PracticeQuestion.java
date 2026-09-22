package com.ieltsaitutor.practice;

import java.util.List;

public record PracticeQuestion(String id, String prompt, List<String> options, String answerKey, String explanation) {}
