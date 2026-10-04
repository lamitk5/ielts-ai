package com.ieltsaitutor.admin.submission;

import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.results.LearnerResultService;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class AdminSubmissionQueryServiceTest {
    @Test
    void learnerCannotReadAdminSubmissionQueue() {
        AdminSubmissionQueryService service = new AdminSubmissionQueryService(
                mock(PracticeSubmissionRepository.class), mock(LearnerResultService.class));
        AuthPrincipal learner = new AuthPrincipal(UUID.randomUUID(), "learner@example.com", "Learner", UserRole.CUSTOMER);
        assertThrows(SecurityException.class, () -> service.list(learner, null, null, 0, 20));
    }

    @Test
    void adminPathRequiresAdminRoleForResultLookup() {
        AdminSubmissionQueryService service = new AdminSubmissionQueryService(
                mock(PracticeSubmissionRepository.class), mock(LearnerResultService.class));
        AuthPrincipal learner = new AuthPrincipal(UUID.randomUUID(), "learner@example.com", "Learner", UserRole.CUSTOMER);
        assertThrows(SecurityException.class, () -> service.result(learner, UUID.randomUUID()));
    }
}
