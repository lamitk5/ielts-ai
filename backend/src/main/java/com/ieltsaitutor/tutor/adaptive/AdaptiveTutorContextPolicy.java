package com.ieltsaitutor.tutor.adaptive;

import com.ieltsaitutor.auth.AuthPrincipal;

/** Central policy boundary for personalized Tutor context. */
public final class AdaptiveTutorContextPolicy {
    private AdaptiveTutorContextPolicy() {}

    public static boolean mayUsePersonalizedContext(AuthPrincipal principal) {
        return principal != null && principal.userId() != null;
    }
}
