package com.ieltsaitutor.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterCommand(@NotBlank @Email String email, @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(max = 120) String firstName) {}
