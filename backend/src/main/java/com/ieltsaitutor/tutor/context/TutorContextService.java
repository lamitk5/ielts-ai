package com.ieltsaitutor.tutor.context;

import com.ieltsaitutor.auth.AuthPrincipal;

public interface TutorContextService {
    TutorLearningContext resolve(AuthPrincipal principal, TutorContextRequest request);
}
