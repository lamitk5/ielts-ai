package com.ieltsaitutor.ai.routing;

import com.ieltsaitutor.ai.exception.AiProviderException;
import com.ieltsaitutor.ai.provider.ProviderId;
import org.springframework.http.HttpStatus;

public class ProviderRoutingPolicy {
    public ProviderFailure classify(ProviderId provider, AiProviderException exception) {
        String code = exception.code();
        ProviderFailureCategory category;
        if ("AI_INVALID_REQUEST".equals(code) || exception.status() == HttpStatus.BAD_REQUEST) {
            category = ProviderFailureCategory.INVALID_REQUEST;
        } else if ("AI_PROVIDER_AUTHENTICATION".equals(code)) {
            category = ProviderFailureCategory.AUTHENTICATION;
        } else if ("AI_PROVIDER_MALFORMED_RESPONSE".equals(code)
                || "AI_PROVIDER_ERROR".equals(code) && exception.status().value() == 502) {
            category = ProviderFailureCategory.MALFORMED_RESPONSE;
        } else if ("AI_CONTENT_BLOCKED".equals(code)) {
            category = ProviderFailureCategory.CONTENT_BLOCKED;
        } else if ("AI_RATE_LIMITED".equals(code) || "AI_TIMEOUT".equals(code)
                || "AI_TEMPORARILY_UNAVAILABLE".equals(code)
                || exception.status().is5xxServerError()) {
            category = ProviderFailureCategory.TRANSIENT;
        } else {
            category = ProviderFailureCategory.INVALID_REQUEST;
        }
        return new ProviderFailure(provider, category, code);
    }

    public boolean shouldAdvance(ProviderFailure failure) {
        return switch (failure.category()) {
            case TRANSIENT, NOT_CONFIGURED, AUTHENTICATION, MALFORMED_RESPONSE -> true;
            case INVALID_REQUEST, CONTENT_BLOCKED -> false;
        };
    }
}
