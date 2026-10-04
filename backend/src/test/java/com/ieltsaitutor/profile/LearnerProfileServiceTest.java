package com.ieltsaitutor.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.auth.AuthException;
import com.ieltsaitutor.auth.AuthUser;
import com.ieltsaitutor.auth.AuthUserRepository;
import com.ieltsaitutor.auth.PasswordHasher;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.learning.intelligence.Skill;
import com.ieltsaitutor.onboarding.LearnerOnboardingProfile;
import com.ieltsaitutor.onboarding.LearnerOnboardingService;
import com.ieltsaitutor.onboarding.OnboardingState;

class LearnerProfileServiceTest {
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
    void getsLearnerProfile() {
        UUID userId = UUID.randomUUID();
        AuthUser user = new AuthUser(userId, "test@example.com", "Lê Văn A", "hash123", UserRole.CUSTOMER, Instant.now());
        LearnerOnboardingProfile onboarding = new LearnerOnboardingProfile(
                userId, "INTERMEDIATE", 7.5, LocalDate.now().plusDays(30), Skill.WRITING,
                45, 5, OnboardingState.COMPLETED, "SELF_REPORTED", "LEARNER_DECLARATION",
                null, null, null, 1L, Instant.now(), Instant.now()
        );

        when(users.findById(userId)).thenReturn(user);
        when(onboardingService.get(userId)).thenReturn(onboarding);
        when(profileRepository.getAvatarUrl(userId)).thenReturn("/avatars/a.png");

        LearnerProfile profile = service.getProfile(userId);

        assertNotNull(profile);
        assertEquals("test@example.com", profile.email());
        assertEquals("Lê Văn A", profile.firstName());
        assertEquals(7.5, profile.targetBand());
        assertEquals(Skill.WRITING, profile.perceivedWeakestSkill());
        assertEquals(45, profile.dailyStudyMinutes());
        assertEquals(5, profile.studyDaysPerWeek());
    }

    @Test
    void updatesLearnerProfile() {
        UUID userId = UUID.randomUUID();
        AuthUser user = new AuthUser(userId, "test@example.com", "Lê Văn A", "hash123", UserRole.CUSTOMER, Instant.now());
        LearnerOnboardingProfile onboarding = LearnerOnboardingProfile.empty(userId, Instant.now());

        when(users.findById(userId)).thenReturn(user);
        when(onboardingService.get(userId)).thenReturn(onboarding);

        UpdateProfileCommand command = new UpdateProfileCommand(
                "Lê Văn B", "/avatar/new.png", 8.0, LocalDate.now().plusDays(60), Skill.SPEAKING, 60, 6, "ADVANCED", OnboardingState.COMPLETED, 0L
        );

        service.updateProfile(userId, command);

        verify(profileRepository).updateDisplayNameAndAvatar(userId, "Lê Văn B", "/avatar/new.png");
        verify(onboardingService).save(eq(userId), any(), eq(0L));
    }

    @Test
    void changesPasswordWithValidCurrentPassword() {
        UUID userId = UUID.randomUUID();
        AuthUser user = new AuthUser(userId, "test@example.com", "Lê Văn A", "oldHash", UserRole.CUSTOMER, Instant.now());

        when(users.findById(userId)).thenReturn(user);
        when(passwordHasher.matches("oldPass123", "oldHash")).thenReturn(true);
        when(passwordHasher.hash("newPass456")).thenReturn("newHash456");

        ChangePasswordCommand command = new ChangePasswordCommand("oldPass123", "newPass456");
        service.changePassword(userId, command);

        verify(profileRepository).updatePasswordHash(userId, "newHash456");
    }

    @Test
    void rejectsPasswordChangeWhenCurrentPasswordIncorrect() {
        UUID userId = UUID.randomUUID();
        AuthUser user = new AuthUser(userId, "test@example.com", "Lê Văn A", "oldHash", UserRole.CUSTOMER, Instant.now());

        when(users.findById(userId)).thenReturn(user);
        when(passwordHasher.matches("wrongPass", "oldHash")).thenReturn(false);

        ChangePasswordCommand command = new ChangePasswordCommand("wrongPass", "newPass456");
        assertThrows(AuthException.class, () -> service.changePassword(userId, command));
    }
}
