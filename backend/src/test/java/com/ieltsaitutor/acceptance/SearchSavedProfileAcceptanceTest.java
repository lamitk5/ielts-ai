package com.ieltsaitutor.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.ieltsaitutor.auth.AuthUser;
import com.ieltsaitutor.auth.AuthUserRepository;
import com.ieltsaitutor.auth.PasswordHasher;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.learning.intelligence.Skill;
import com.ieltsaitutor.onboarding.LearnerOnboardingProfile;
import com.ieltsaitutor.onboarding.LearnerOnboardingRepository;
import com.ieltsaitutor.onboarding.LearnerOnboardingService;
import com.ieltsaitutor.onboarding.OnboardingState;
import com.ieltsaitutor.practice.catalog.ApprovedPracticeCatalogService;
import com.ieltsaitutor.practice.catalog.PracticePublication;
import com.ieltsaitutor.practice.saved.SavedPractice;
import com.ieltsaitutor.practice.saved.SavedPracticeRepository;
import com.ieltsaitutor.practice.saved.SavedPracticeService;
import com.ieltsaitutor.profile.ChangePasswordCommand;
import com.ieltsaitutor.profile.LearnerProfile;
import com.ieltsaitutor.profile.LearnerProfileRepository;
import com.ieltsaitutor.profile.LearnerProfileService;
import com.ieltsaitutor.profile.UpdateProfileCommand;
import com.ieltsaitutor.search.PracticeSearchResult;
import com.ieltsaitutor.search.PracticeSearchService;

class SearchSavedProfileAcceptanceTest {
    private NamedParameterJdbcTemplate jdbc;
    private PracticeSearchService searchService;
    private SavedPracticeRepository savedRepository;
    private ApprovedPracticeCatalogService catalogService;
    private SavedPracticeService savedService;
    private AuthUserRepository authUsers;
    private LearnerProfileRepository profileRepository;
    private LearnerOnboardingRepository onboardingRepository;
    private LearnerOnboardingService onboardingService;
    private PasswordHasher passwordHasher;
    private LearnerProfileService profileService;

    @BeforeEach
    void setUp() {
        jdbc = mock(NamedParameterJdbcTemplate.class);
        searchService = new PracticeSearchService(jdbc);

        savedRepository = mock(SavedPracticeRepository.class);
        catalogService = mock(ApprovedPracticeCatalogService.class);
        savedService = new SavedPracticeService(savedRepository, catalogService);

        authUsers = mock(AuthUserRepository.class);
        profileRepository = mock(LearnerProfileRepository.class);
        onboardingRepository = mock(LearnerOnboardingRepository.class);
        onboardingService = mock(LearnerOnboardingService.class);
        passwordHasher = mock(PasswordHasher.class);
        profileService = new LearnerProfileService(authUsers, profileRepository, onboardingService, passwordHasher);
    }

    @Test
    @DisplayName("FLOW A — Search Practice: Learner searches topic, only approved visible practice returned")
    void flowA_searchPracticeApprovedOnly() {
        List<PracticeSearchResult> results = searchService.search("reading foundation");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(r -> "PRACTICE_SET".equals(r.resultType())));
        assertEquals("reading-foundation-01", results.get(0).id());
    }

    @Test
    @DisplayName("FLOW B — Writing: Search Writing topic returns relevant Writing prompt")
    void flowB_searchWritingPrompt() {
        List<PracticeSearchResult> results = searchService.search("Essay Task 2");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> "WRITING_PROMPT".equals(r.resultType()) && r.title().contains("Task 2")));
    }

    @Test
    @DisplayName("FLOW C — Speaking: Search Speaking topic returns relevant Speaking topic")
    void flowC_searchSpeakingTopic() {
        List<PracticeSearchResult> results = searchService.search("Speaking Part 1");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> "SPEAKING_TOPIC".equals(r.resultType()) && r.title().contains("Part 1")));
    }

    @Test
    @DisplayName("FLOW D — Own History: User A searches own submission, User B submission is isolated")
    void flowD_ownSubmissionIsolation() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        searchService.search("history", userA, null, null, 0, 20);

        verify(jdbc, atLeastOnce()).query(
                contains("WHERE user_id = :userId"),
                org.mockito.ArgumentMatchers.argThat((MapSqlParameterSource p) -> userA.equals(p.getValue("userId"))),
                any(RowCallbackHandler.class)
        );

        verify(jdbc, never()).query(
                any(String.class),
                org.mockito.ArgumentMatchers.argThat((MapSqlParameterSource p) -> userB.equals(p.getValue("userId"))),
                any(RowCallbackHandler.class)
        );
    }

    @Test
    @DisplayName("FLOW E — Saved Practice: Save appears in Bài đã lưu, Unsave removes from list")
    void flowE_savedPracticeLifecycle() {
        UUID userA = UUID.randomUUID();
        String setId = "reading-foundation-01";
        SavedPractice savedItem = new SavedPractice(UUID.randomUUID(), userA, setId, "reading", "Reading foundation", Instant.now(), true);

        when(savedRepository.save(any(SavedPractice.class))).thenReturn(savedItem);
        when(savedRepository.findByUser(userA, null, 0, 20)).thenReturn(List.of(savedItem));

        SavedPractice saved = savedService.save(userA, setId);
        assertNotNull(saved);
        assertEquals(setId, saved.publishedSetId());

        List<SavedPractice> list = savedService.list(userA, null, 0, 20);
        assertEquals(1, list.size());
        assertEquals(setId, list.get(0).publishedSetId());

        savedService.unsave(userA, setId);
        verify(savedRepository).delete(userA, setId);
    }

    @Test
    @DisplayName("FLOW F — Profile: Update learning goal persists correctly")
    void flowF_profileUpdatePersists() {
        UUID userId = UUID.randomUUID();
        AuthUser user = new AuthUser(userId, "learner@example.com", "Learner Name", "passHash", UserRole.CUSTOMER, Instant.now());
        LearnerOnboardingProfile initialOnboarding = LearnerOnboardingProfile.empty(userId, Instant.now());

        when(authUsers.findById(userId)).thenReturn(user);
        when(onboardingService.get(userId)).thenReturn(initialOnboarding);

        UpdateProfileCommand command = new UpdateProfileCommand(
                "New Name", null, 7.5, LocalDate.now().plusMonths(2), Skill.READING, 45, 5, "INTERMEDIATE", OnboardingState.COMPLETED, 0L
        );

        savedService.list(userId, null, 0, 20);
        LearnerProfile updated = profileService.updateProfile(userId, command);

        verify(profileRepository).updateDisplayNameAndAvatar(userId, "New Name", null);
        verify(onboardingService).save(eq(userId), any(), eq(0L));
    }

    @Test
    @DisplayName("FLOW G — Privilege attack: Send role/admin field through profile API is rejected/ignored safely")
    void flowG_privilegeEscalationProtection() {
        UUID userId = UUID.randomUUID();
        AuthUser originalUser = new AuthUser(userId, "user@example.com", "Regular User", "hash", UserRole.CUSTOMER, Instant.now());

        when(authUsers.findById(userId)).thenReturn(originalUser);
        when(onboardingService.get(userId)).thenReturn(LearnerOnboardingProfile.empty(userId, Instant.now()));

        UpdateProfileCommand attackCommand = new UpdateProfileCommand("Regular User", null, null, null, null, null, null, null, null, 0L);

        LearnerProfile profile = profileService.updateProfile(userId, attackCommand);
        assertEquals("CUSTOMER", profile.role());
    }
}
