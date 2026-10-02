package com.ieltsaitutor.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.auth.AuthException;
import com.ieltsaitutor.learning.intelligence.Skill;

class OnboardingProfileServiceTest {

    private final MemoryRepository repository = new MemoryRepository();
    private final LearnerOnboardingService service = new LearnerOnboardingService(repository);

    @Test
    void firstReadCreatesAnIncompleteOwnerScopedProfileWithNoSelfReport() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        LearnerOnboardingProfile created = service.get(first);

        assertThat(created.userId()).isEqualTo(first);
        assertThat(created.state()).isEqualTo(OnboardingState.NOT_STARTED);
        assertThat(created.selfReportedLevel()).isNull();
        assertThat(created.targetBand()).isNull();
        assertThat(created.targetExamDate()).isNull();
        assertThat(created.perceivedWeakestSkill()).isNull();
        assertThat(created.dailyStudyMinutes()).isNull();
        assertThat(created.studyDaysPerWeek()).isNull();
        assertThat(created.version()).isZero();
        assertThat(created.updatedAt()).isNotNull();

        UUID other = UUID.randomUUID();
        assertThat(service.get(other).userId()).isEqualTo(other);
        assertThat(other).isNotEqualTo(first);
        assertThat(repository.rows).hasSize(2);
    }

    @Test
    void savingGoalsStoresEveryOnboardingFieldAndIncrementsVersion() {
        UUID userId = UUID.randomUUID();
        service.get(userId);

        LearnerOnboardingProfile saved = service.save(userId,
                new OnboardingGoalCommand("INTERMEDIATE", 6.5, LocalDate.of(2026, 12, 5),
                        Skill.READING, 45, 5, OnboardingState.COMPLETED), 0);

        assertThat(saved.selfReportedLevel()).isEqualTo("INTERMEDIATE");
        assertThat(saved.targetBand()).isEqualTo(6.5);
        assertThat(saved.targetExamDate()).isEqualTo(LocalDate.of(2026, 12, 5));
        assertThat(saved.perceivedWeakestSkill()).isEqualTo(Skill.READING);
        assertThat(saved.dailyStudyMinutes()).isEqualTo(45);
        assertThat(saved.studyDaysPerWeek()).isEqualTo(5);
        assertThat(saved.state()).isEqualTo(OnboardingState.COMPLETED);
        assertThat(saved.version()).isEqualTo(1);
        assertThat(saved.updatedAt()).isNotNull();
    }

    @Test
    void examDateIsOptionalButPartialSetupStaysResumable() {
        UUID userId = UUID.randomUUID();
        service.get(userId);

        LearnerOnboardingProfile saved = service.save(userId,
                new OnboardingGoalCommand("BEGINNER", 5.5, null, Skill.WRITING, 30, 4,
                        OnboardingState.IN_PROGRESS), 0);

        assertThat(saved.targetExamDate()).isNull();
        assertThat(saved.state()).isEqualTo(OnboardingState.IN_PROGRESS);
        assertThat(saved.version()).isEqualTo(1);
    }

    @Test
    void skipIsRecordedIdempotentlyWithoutInventingGoals() {
        UUID userId = UUID.randomUUID();
        service.get(userId);

        LearnerOnboardingProfile skipped = service.complete(userId, OnboardingState.SKIPPED, 0);
        LearnerOnboardingProfile again = service.complete(userId, OnboardingState.SKIPPED, skipped.version());

        assertThat(skipped.state()).isEqualTo(OnboardingState.SKIPPED);
        assertThat(skipped.selfReportedLevel()).isNull();
        assertThat(skipped.targetBand()).isNull();
        assertThat(skipped.dailyStudyMinutes()).isNull();
        assertThat(again.state()).isEqualTo(OnboardingState.SKIPPED);
        assertThat(again.version()).isEqualTo(skipped.version());
    }

    @Test
    void editLaterCreatesANewVersionAndKeepsPriorHistory() {
        UUID userId = UUID.randomUUID();
        service.get(userId);
        service.save(userId, new OnboardingGoalCommand("BEGINNER", 5.0, null, Skill.LISTENING, 30, 3,
                OnboardingState.COMPLETED), 0);

        service.save(userId, new OnboardingGoalCommand("ADVANCED", 7.5, LocalDate.of(2027, 3, 1),
                Skill.WRITING, 90, 6, OnboardingState.COMPLETED), 1);

        assertThat(service.get(userId).targetBand()).isEqualTo(7.5);
        assertThat(service.get(userId).version()).isEqualTo(2);
        assertThat(repository.versionsOf(userId)).containsExactly(5.0, 7.5);
    }

    @Test
    void staleVersionCannotOverwriteAConcurrentEdit() {
        UUID userId = UUID.randomUUID();
        service.get(userId);
        service.save(userId, new OnboardingGoalCommand("BEGINNER", 5.0, null, Skill.READING, 30, 3,
                OnboardingState.COMPLETED), 0);

        assertThatThrownBy(() -> service.save(userId,
                new OnboardingGoalCommand("ADVANCED", 8.0, null, Skill.SPEAKING, 60, 5,
                        OnboardingState.COMPLETED), 0))
                .isInstanceOfSatisfying(AuthException.class,
                        error -> assertThat(error.code()).isEqualTo("ONBOARDING_VERSION_CONFLICT"));
        assertThat(service.get(userId).targetBand()).isEqualTo(5.0);
    }

    @Test
    void targetBandOutsideTheConfiguredIeltsRangeIsRejected() {
        UUID userId = UUID.randomUUID();
        service.get(userId);

        assertThatThrownBy(() -> service.save(userId,
                new OnboardingGoalCommand("INTERMEDIATE", 12.0, null, Skill.READING, 30, 3,
                        OnboardingState.COMPLETED), 0))
                .isInstanceOfSatisfying(AuthException.class,
                        error -> assertThat(error.code()).isEqualTo("ONBOARDING_INVALID_REQUEST"));
        assertThatThrownBy(() -> service.save(userId,
                new OnboardingGoalCommand("INTERMEDIATE", -1.0, null, Skill.READING, 30, 3,
                        OnboardingState.COMPLETED), 0))
                .isInstanceOfSatisfying(AuthException.class,
                        error -> assertThat(error.code()).isEqualTo("ONBOARDING_INVALID_REQUEST"));
        assertThatThrownBy(() -> service.save(userId,
                new OnboardingGoalCommand("INTERMEDIATE", 6.3, null, Skill.READING, 30, 3,
                        OnboardingState.COMPLETED), 0))
                .isInstanceOfSatisfying(AuthException.class,
                        error -> assertThat(error.code()).isEqualTo("ONBOARDING_INVALID_REQUEST"));
    }

    @Test
    void everyConfiguredBandStepIsAccepted() {
        UUID userId = UUID.randomUUID();
        service.get(userId);
        long version = 0;

        for (double band = 0.0; band <= 9.0; band += 0.5) {
            service.save(userId, new OnboardingGoalCommand("INTERMEDIATE", band, null, Skill.READING,
                    30, 3, OnboardingState.COMPLETED), version);
            version++;
        }

        assertThat(service.get(userId).targetBand()).isEqualTo(9.0);
        assertThat(service.get(userId).version()).isEqualTo(19);
    }

    @Test
    void outOfRangeScheduleAndUnknownLevelAreRejected() {
        UUID userId = UUID.randomUUID();
        service.get(userId);

        assertThatThrownBy(() -> service.save(userId,
                new OnboardingGoalCommand("INTERMEDIATE", 6.0, null, Skill.READING, 0, 3,
                        OnboardingState.COMPLETED), 0))
                .isInstanceOfSatisfying(AuthException.class,
                        error -> assertThat(error.code()).isEqualTo("ONBOARDING_INVALID_REQUEST"));
        assertThatThrownBy(() -> service.save(userId,
                new OnboardingGoalCommand("INTERMEDIATE", 6.0, null, Skill.READING, 45, 8,
                        OnboardingState.COMPLETED), 0))
                .isInstanceOfSatisfying(AuthException.class,
                        error -> assertThat(error.code()).isEqualTo("ONBOARDING_INVALID_REQUEST"));
        assertThatThrownBy(() -> service.save(userId,
                new OnboardingGoalCommand("GURU", 6.0, null, Skill.READING, 45, 3,
                        OnboardingState.COMPLETED), 0))
                .isInstanceOfSatisfying(AuthException.class,
                        error -> assertThat(error.code()).isEqualTo("ONBOARDING_INVALID_REQUEST"));
        assertThat(service.get(userId).version()).isZero();
    }

    @Test
    void selfReportedWeaknessIsNeverWrittenIntoMeasuredAdaptiveEvidence() {
        UUID userId = UUID.randomUUID();
        service.get(userId);
        service.save(userId, new OnboardingGoalCommand("BEGINNER", 5.0, null, Skill.READING, 30, 3,
                OnboardingState.COMPLETED), 0);

        LearnerOnboardingProfile stored = service.get(userId);
        assertThat(stored.perceivedWeakestSkill()).isEqualTo(Skill.READING);
        assertThat(stored.measuredWeakestSkill()).isNull();
        assertThat(stored.source()).isEqualTo("SELF_REPORTED");
        assertThat(repository.touchedTables).containsExactly("learner_onboarding_profiles");
    }

    @Test
    void onboardingServiceCannotDependOnAdaptiveProfileOrMistakeWriters() {
        List<Class<?>> collaborators = new ArrayList<>();
        for (Constructor<?> constructor : LearnerOnboardingService.class.getDeclaredConstructors()) {
            for (Class<?> parameter : constructor.getParameterTypes()) {
                collaborators.add(parameter);
            }
        }

        assertThat(collaborators).isNotEmpty();
        assertThat(collaborators).allSatisfy(type -> assertThat(type.getName()).doesNotContain(
                "LearningIntelligenceRepository", "LearningProfileService", "MistakeRecordService",
                "JdbcLearningIntelligenceRepository", "WeaknessStrengthAnalyzer", "TrendAnalyzer"));
    }

    @Test
    void selfReportedLevelIsNeverPresentedAsAMeasuredAbility() {
        UUID userId = UUID.randomUUID();
        service.get(userId);
        LearnerOnboardingProfile saved = service.save(userId,
                new OnboardingGoalCommand("ADVANCED", 8.0, null, Skill.SPEAKING, 60, 5,
                        OnboardingState.COMPLETED), 0);

        assertThat(saved.source()).isEqualTo("SELF_REPORTED");
        assertThat(saved.basis()).isEqualTo("LEARNER_DECLARATION");
        assertThat(saved.selfReportedLevel()).isEqualTo("ADVANCED");
        assertThat(saved.measuredLevel()).isNull();
        assertThat(saved.evidenceReference()).isNull();
    }

    @Test
    void completingSetupRequiresTheEssentialGoalFields() {
        UUID userId = UUID.randomUUID();
        service.get(userId);

        assertThatThrownBy(() -> service.complete(userId, OnboardingState.COMPLETED, 0))
                .isInstanceOfSatisfying(AuthException.class,
                        error -> assertThat(error.code()).isEqualTo("ONBOARDING_INVALID_REQUEST"));
        assertThatThrownBy(() -> service.save(userId,
                new OnboardingGoalCommand("BEGINNER", null, null, Skill.READING, 30, 3,
                        OnboardingState.COMPLETED), 0))
                .isInstanceOfSatisfying(AuthException.class,
                        error -> assertThat(error.code()).isEqualTo("ONBOARDING_INVALID_REQUEST"));
        assertThat(service.get(userId).state()).isEqualTo(OnboardingState.NOT_STARTED);
    }

    private static final class MemoryRepository implements LearnerOnboardingRepository {
        private final Map<UUID, LearnerOnboardingProfile> rows = new HashMap<>();
        private final List<Map.Entry<UUID, LearnerOnboardingProfile>> history = new ArrayList<>();
        private final List<String> touchedTables = new ArrayList<>();

        @Override
        public LearnerOnboardingProfile find(UUID userId) {
            return rows.get(userId);
        }

        @Override
        public void insertDefault(UUID userId, Instant now) {
            if (rows.containsKey(userId)) {
                return;
            }
            LearnerOnboardingProfile created = LearnerOnboardingProfile.empty(userId, now);
            rows.put(userId, created);
            history.add(Map.entry(userId, created));
            touchedTables.add("learner_onboarding_profiles");
        }

        @Override
        public boolean update(UUID userId, LearnerOnboardingProfile profile, long expectedVersion) {
            LearnerOnboardingProfile current = rows.get(userId);
            if (current == null || current.version() != expectedVersion) {
                return false;
            }
            rows.put(userId, profile);
            history.add(Map.entry(userId, profile));
            return true;
        }

        List<Double> versionsOf(UUID userId) {
            return history.stream().filter(entry -> entry.getKey().equals(userId))
                    .map(entry -> entry.getValue()).filter(profile -> profile.targetBand() != null)
                    .map(LearnerOnboardingProfile::targetBand).toList();
        }
    }
}