package com.ieltsaitutor.practice.generator.validator;

import com.ieltsaitutor.practice.generator.domain.ValidationStatus;

public record ValidationFinding(
        String code,
        ValidationStatus severity,
        String targetId,
        String message,
        String heuristicDetails) {

    public ValidationFinding {
        if (severity == null) severity = ValidationStatus.FAIL;
        if (heuristicDetails == null) heuristicDetails = "";
    }
}
