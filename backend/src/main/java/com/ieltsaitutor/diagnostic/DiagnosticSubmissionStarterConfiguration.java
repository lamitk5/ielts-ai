package com.ieltsaitutor.diagnostic;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ieltsaitutor.submission.CanonicalSubmissionService;
import com.ieltsaitutor.submission.SubmissionStartCommand;

/**
 * Starts diagnostic sections through the canonical Phase 4 submission engine so
 * the diagnostic never grows a private answer, autosave or scoring path.
 */
@Configuration
public class DiagnosticSubmissionStarterConfiguration {

    @Bean
    DiagnosticSubmissionStarter diagnosticSubmissionStarter(CanonicalSubmissionService submissions) {
        return (UUID ownerId, SubmissionStartCommand command) -> submissions.start(ownerId, command).id();
    }
}