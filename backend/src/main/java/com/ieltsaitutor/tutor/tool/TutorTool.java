package com.ieltsaitutor.tutor.tool;

import com.ieltsaitutor.tutor.context.TutorLearningContext;
import com.ieltsaitutor.tutor.intent.TutorIntent;

public interface TutorTool {
    boolean supports(TutorIntent intent);
    TutorToolResult execute(TutorLearningContext context);
}
