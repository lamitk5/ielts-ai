package com.ieltsaitutor.onboarding;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.auth.AuthException;

@Service
public class LearnerOnboardingService {
    static final Set<String> LEVELS = Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED");
    static final int MIN_DAILY_MINUTES = 5;
    static final int MAX_DAILY_MINUTES = 240;
    static final int MIN_STUDY_DAYS = 1;
    static final int MAX_STUDY_DAYS = 7;
    static final double MIN_BAND = 0.0;
    static final double MAX_BAND = 9.0;
    static final double BAND_STEP = 0.5;

    private static final String SOURCE = "SELF_REPORTED";
    private static final String BASIS = "LEARNER_DECLARATION";

    private final LearnerOnboardingRepository repository;
    private final Clock clock;

    @Autowired
    public LearnerOnboardingService(LearnerOnboardingRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public LearnerOnboardingService(LearnerOnboardingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public LearnerOnboardingProfile get(UUID userId) {
        requireOwner(userId);
        LearnerOnboardingProfile existing = repository.find(userId);
        if (existing != null) {
            return existing;
        }
        repository.insertDefault(userId, now());
        return repository.find(userId);
    }

    public LearnerOnboardingProfile save(UUID userId, OnboardingGoalCommand command, long expectedVersion) {
        requireOwner(userId);
        validate(command, expectedVersion);
        get(userId);
        Instant at = now();
        LearnerOnboardingProfile current = repository.find(userId);
        LearnerOnboardingProfile updated = new LearnerOnboardingProfile(userId, command.selfReportedLevel(),
                command.targetBand(), command.targetExamDate(), command.perceivedWeakestSkill(),
                command.dailyStudyMinutes(), command.studyDaysPerWeek(), command.state(), SOURCE, BASIS,
                current.measuredLevel(), current.measuredWeakestSkill(), current.evidenceReference(),
                current.version() + 1, current.createdAt(), at);
        if (!repository.update(userId, updated, expectedVersion)) {
            throw new AuthException("ONBOARDING_VERSION_CONFLICT", HttpStatus.CONFLICT,
                    "Thông tin mục tiêu đã được thay đổi. Vui lòng tải lại.");
        }
        return repository.find(userId);
    }

    public LearnerOnboardingProfile complete(UUID userId, OnboardingState state, long expectedVersion) {
        requireOwner(userId);
        if (state != OnboardingState.COMPLETED && state != OnboardingState.SKIPPED) {
            throw invalid();
        }
        LearnerOnboardingProfile current = get(userId);
        if (current.state() == state) {
            return current;
        }
        return save(userId, new OnboardingGoalCommand(current.selfReportedLevel(), current.targetBand(),
                current.targetExamDate(), current.perceivedWeakestSkill(), current.dailyStudyMinutes(),
                current.studyDaysPerWeek(), state), expectedVersion);
    }

    private void validate(OnboardingGoalCommand command, long expectedVersion) {
        if (command == null || expectedVersion < 0 || command.state() == null) {
            throw invalid();
        }
        if (command.selfReportedLevel() != null && !LEVELS.contains(command.selfReportedLevel())
                || command.targetBand() != null && !validBand(command.targetBand())
                || command.targetExamDate() != null
                        && command.targetExamDate().isBefore(java.time.LocalDate.now(clock))
                || command.dailyStudyMinutes() != null && !validMinutes(command.dailyStudyMinutes())
                || command.studyDaysPerWeek() != null && !validStudyDays(command.studyDaysPerWeek())) {
            throw invalid();
        }
        if (command.state() == OnboardingState.COMPLETED
                && (command.selfReportedLevel() == null || command.targetBand() == null
                        || !validMinutes(command.dailyStudyMinutes())
                        || !validStudyDays(command.studyDaysPerWeek()))) {
            throw invalid();
        }
    }

    private static boolean validBand(Double band) {
        if (band == null || band < MIN_BAND || band > MAX_BAND) {
            return false;
        }
        return Math.abs(Math.round(band / BAND_STEP) * BAND_STEP - band) < 1e-9;
    }

    private static boolean validMinutes(Integer minutes) {
        return minutes != null && minutes >= MIN_DAILY_MINUTES && minutes <= MAX_DAILY_MINUTES;
    }

    private static boolean validStudyDays(Integer days) {
        return days != null && days >= MIN_STUDY_DAYS && days <= MAX_STUDY_DAYS;
    }

    private static void requireOwner(UUID userId) {
        if (userId == null) {
            throw new AuthException("AUTH_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
        }
    }

    private static AuthException invalid() {
        return new AuthException("ONBOARDING_INVALID_REQUEST", HttpStatus.BAD_REQUEST,
                "Thông tin mục tiêu chưa hợp lệ.");
    }

    private Instant now() { return Instant.now(clock); }

    static List<String> allowedLevels() { return List.copyOf(LEVELS); }
}