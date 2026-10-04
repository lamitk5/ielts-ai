package com.ieltsaitutor.rag.admin;

public interface AdminAuthorizationService {
    AuthorizationDecision authorize(String providedToken);
}
