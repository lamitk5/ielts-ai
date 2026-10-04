package com.ieltsaitutor.profile;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.auth.AuthException;
import com.ieltsaitutor.auth.AuthUser;
import com.ieltsaitutor.auth.AuthUserRepository;
import com.ieltsaitutor.auth.PasswordHasher;
import com.ieltsaitutor.onboarding.LearnerOnboardingProfile;
import com.ieltsaitutor.onboarding.LearnerOnboardingService;
import com.ieltsaitutor.onboarding.OnboardingGoalCommand;
import com.ieltsaitutor.onboarding.OnboardingState;

@Service
public class LearnerProfileService {
    private final AuthUserRepository users;
    private final LearnerProfileRepository profileRepository;
    private final LearnerOnboardingService onboardingService;
    private final PasswordHasher passwordHasher;

    public LearnerProfileService(
            AuthUserRepository users,
            LearnerProfileRepository profileRepository,
            LearnerOnboardingService onboardingService,
            PasswordHasher passwordHasher) {
        this.users = users;
        this.profileRepository = profileRepository;
        this.onboardingService = onboardingService;
        this.passwordHasher = passwordHasher;
    }

    public LearnerProfile getProfile(UUID userId) {
        AuthUser user = requireUser(userId);
        LearnerOnboardingProfile onboarding = onboardingService.get(userId);
        String avatarUrl = profileRepository.getAvatarUrl(userId);

        return new LearnerProfile(
                user.id(),
                user.email(),
                user.firstName(),
                avatarUrl,
                user.role().name(),
                onboarding != null ? onboarding.targetBand() : null,
                onboarding != null ? onboarding.targetExamDate() : null,
                onboarding != null ? onboarding.perceivedWeakestSkill() : null,
                onboarding != null ? onboarding.dailyStudyMinutes() : null,
                onboarding != null ? onboarding.studyDaysPerWeek() : null,
                onboarding != null ? onboarding.selfReportedLevel() : null,
                onboarding != null ? onboarding.state() : OnboardingState.NOT_STARTED,
                onboarding != null ? onboarding.version() : 0L,
                user.createdAt(),
                onboarding != null ? onboarding.updatedAt() : Instant.now()
        );
    }

    public LearnerProfile updateProfile(UUID userId, UpdateProfileCommand command) {
        AuthUser user = requireUser(userId);

        if (command == null) {
            throw new AuthException("PROFILE_INVALID_REQUEST", HttpStatus.BAD_REQUEST, "Dữ liệu cập nhật không hợp lệ.");
        }

        // 1. Update display name / avatar if provided
        String newFirstName = user.firstName();
        if (command.firstName() != null) {
            String trimmed = command.firstName().trim();
            if (trimmed.isBlank() || trimmed.length() > 120) {
                throw new AuthException("PROFILE_INVALID_REQUEST", HttpStatus.BAD_REQUEST, "Tên hiển thị không hợp lệ.");
            }
            newFirstName = trimmed;
        }
        String newAvatar = command.avatarUrl();
        profileRepository.updateDisplayNameAndAvatar(userId, newFirstName, newAvatar);

        // 2. Update onboarding goals if any goal field is supplied
        LearnerOnboardingProfile currentOnboarding = onboardingService.get(userId);
        long version = command.onboardingVersion() != null ? command.onboardingVersion() : currentOnboarding.version();

        Double targetBand = command.targetBand() != null ? command.targetBand() : currentOnboarding.targetBand();
        java.time.LocalDate examDate = command.targetExamDate() != null ? command.targetExamDate() : currentOnboarding.targetExamDate();
        com.ieltsaitutor.learning.intelligence.Skill weakestSkill = command.perceivedWeakestSkill() != null ? command.perceivedWeakestSkill() : currentOnboarding.perceivedWeakestSkill();
        Integer dailyMinutes = command.dailyStudyMinutes() != null ? command.dailyStudyMinutes() : currentOnboarding.dailyStudyMinutes();
        Integer studyDays = command.studyDaysPerWeek() != null ? command.studyDaysPerWeek() : currentOnboarding.studyDaysPerWeek();
        String level = command.selfReportedLevel() != null ? command.selfReportedLevel() : currentOnboarding.selfReportedLevel();
        OnboardingState state = command.onboardingState() != null ? command.onboardingState() : currentOnboarding.state();

        OnboardingGoalCommand goalCommand = new OnboardingGoalCommand(
                level, targetBand, examDate, weakestSkill, dailyMinutes, studyDays,
                state == OnboardingState.NOT_STARTED ? OnboardingState.IN_PROGRESS : state
        );
        onboardingService.save(userId, goalCommand, version);

        return getProfile(userId);
    }

    public void changePassword(UUID userId, ChangePasswordCommand command) {
        AuthUser user = requireUser(userId);

        if (command.currentPassword() == null || command.currentPassword().isBlank()) {
            throw new AuthException("PASSWORD_INVALID_CURRENT", HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không được để trống.");
        }
        if (command.newPassword() == null || command.newPassword().isBlank() || command.newPassword().length() < 8) {
            throw new AuthException("PASSWORD_TOO_SHORT", HttpStatus.BAD_REQUEST, "Mật khẩu mới phải có ít nhất 8 ký tự.");
        }

        if (!passwordHasher.matches(command.currentPassword(), user.passwordHash())) {
            throw new AuthException("AUTH_INVALID_CREDENTIALS", HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không chính xác.");
        }

        String newHash = passwordHasher.hash(command.newPassword());
        profileRepository.updatePasswordHash(userId, newHash);
    }

    private AuthUser requireUser(UUID userId) {
        if (userId == null) {
            throw new AuthException("AUTH_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
        }
        AuthUser user = users.findById(userId);
        if (user == null) {
            throw new AuthException("AUTH_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại.");
        }
        return user;
    }
}
