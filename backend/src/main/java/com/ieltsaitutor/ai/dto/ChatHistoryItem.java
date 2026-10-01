package com.ieltsaitutor.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatHistoryItem(
        @NotBlank @Pattern(regexp = "USER|ASSISTANT", message = "history.role is not supported") String role,
        @NotBlank @Size(max = 4000) String content
) {
}
