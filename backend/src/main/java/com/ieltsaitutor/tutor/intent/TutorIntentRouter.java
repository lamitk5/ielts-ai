package com.ieltsaitutor.tutor.intent;

import com.ieltsaitutor.ai.dto.AiChatRequest;
import com.ieltsaitutor.tutor.context.TutorLearningContext;

public interface TutorIntentRouter {
    TutorIntentRoute route(AiChatRequest request, TutorLearningContext context);
}
