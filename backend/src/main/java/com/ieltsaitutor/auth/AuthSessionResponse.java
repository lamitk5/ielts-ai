package com.ieltsaitutor.auth;

public record AuthSessionResponse(String token, AuthUserView user) {}
