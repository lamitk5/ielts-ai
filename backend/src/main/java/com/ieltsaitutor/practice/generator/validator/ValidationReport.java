package com.ieltsaitutor.practice.generator.validator;

import java.util.List;

import com.ieltsaitutor.practice.generator.domain.ValidationStatus;

public record ValidationReport(
        String validatorName,
        String policyVersion,
        ValidationStatus status,
        List<ValidationFinding> findings) {

    public ValidationReport {
        if (findings == null) findings = List.of();
    }
}
