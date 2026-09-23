package com.ieltsaitutor.ai.routing;

public enum ProviderFailureCategory {
    TRANSIENT,
    NOT_CONFIGURED,
    AUTHENTICATION,
    MALFORMED_RESPONSE,
    INVALID_REQUEST,
    CONTENT_BLOCKED
}
