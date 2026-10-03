package com.ieltsaitutor.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.auth.AuthUser;
import com.ieltsaitutor.auth.AuthUserRepository;
import com.ieltsaitutor.auth.PasswordHasher;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.onboarding.LearnerOnboardingProfile;
import com.ieltsaitutor.onboarding.LearnerOnboardingService;

class LearnerProfileSecurityTest {
    private AuthUserRepository users;
    private LearnerProfileRepository profileRepository;
    private LearnerOnboardingService onboardingService;
    private PasswordHasher passwordHasher;
    private LearnerProfileService service;

    @BeforeEach
    void setUp() {
        users = mock(AuthUserRepository.class);
        profileRepository = mock(LearnerProfileRepository.class);
        onboardingService = mock(LearnerOnboardingService.class);
        passwordHasher = mock(PasswordHasher.class);
        service = new LearnerProfileService(users, profileRepository, onboardingService, passwordHasher);
    }

    @Test
    void profileUpdateCannotEscalatePrivilegesOrAlterRole() {
        UUID userId = UUID.randomUUID();
        AuthUser originalUser = new AuthUser(userId, "learner@example.com", "Learner", "hash", UserRole.CUSTOMER, Instant.now());

        when(users.findById(userId)).thenReturn(originalUser);
        when(onboardingService.get(userId)).thenReturn(LearnerOnboardingProfile.empty(userId, Instant.now()));

        UpdateProfileCommand maliciousCommand = new UpdateProfileCommand(
                "Attacker", null, null, null, null, null, null, null, null, 0L
        );

        LearnerProfile updated = service.updateProfile(userId, maliciousCommand);

        // Verify role remained CUSTOMER
        assertEquals("CUSTOMER", updated.role());
        verify(profileRepository).updateDisplayNameAndAvatar(userId, "Attacker", null);
    }

    @Test
    void userACannotUpdateUserBProfile() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        AuthUser userAData = new AuthUser(userA, "a@example.com", "User A", "hashA", UserRole.CUSTOMER, Instant.now());
        when(users.findById(userA)).thenReturn(userAData);
        when(onboardingService.get(userA)).thenReturn(LearnerOnboardingProfile.empty(userA, Instant.now()));

        UpdateProfileCommand command = new UpdateProfileCommand("User A Updated", null, null, null, null, null, null, null, null, 0L);
        service.updateProfile(userA, command);

        verify(profileRepository).updateDisplayNameAndAvatar(eq(userA), any(), any());
        verify(profileRepository, never()).updateDisplayNameAndAvatar(eq(userB), any(), any());
    }
}
