package com.ieltsaitutor.practice.generator.dto;

import java.util.List;
import com.ieltsaitutor.practice.generator.ai.RawPassagePayload;
import com.ieltsaitutor.practice.generator.ai.RawQuestionPayload;

public record ManualEditSetRequest(
    RawPassagePayload passage,
    List<RawQuestionPayload> questions,
    String editNotes
) {}
