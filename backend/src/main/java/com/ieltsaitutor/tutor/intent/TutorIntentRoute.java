package com.ieltsaitutor.tutor.intent;

public record TutorIntentRoute(TutorIntent intent, boolean externalAiAllowed, boolean ragAllowed, String reason) {}
