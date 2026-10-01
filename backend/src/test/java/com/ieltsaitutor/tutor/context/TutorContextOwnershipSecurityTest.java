package com.ieltsaitutor.tutor.context;

import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.learning.LearningActivity;
import com.ieltsaitutor.learning.LearningAttempt;
import com.ieltsaitutor.learning.LearningRepository;
import com.ieltsaitutor.practice.PracticeAttemptStore;
import com.ieltsaitutor.practice.SyntheticPracticeCatalog;
import com.ieltsaitutor.speaking.SpeakingAttempt;
import com.ieltsaitutor.speaking.SpeakingRepository;
import com.ieltsaitutor.writing.WritingAssessment;
import com.ieltsaitutor.writing.WritingRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TutorContextOwnershipSecurityTest {
    private final UUID owner = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final TutorContextService service = new DefaultTutorContextService(
            SyntheticPracticeCatalog.inMemory(), emptyLearning(), emptyWriting(), emptySpeaking(), emptyAttempts());

    @Test
    void privateContextRequiresAuthentication() {
        assertThatThrownBy(() -> service.resolve(null,
                new TutorContextRequest("writing", null, null, UUID.randomUUID(), "task-1", null)))
                .isInstanceOf(TutorContextException.class)
                .hasMessageContaining("Đăng nhập");
    }

    @Test
    void crossUserPracticeAttemptCannotBecomeTutorContext() {
        TutorLearningContext context = service.resolve(principal(owner),
                new TutorContextRequest("reading", "reading-foundation-01", "reading-q1", null, null, null));

        assertThat(context.available()).isTrue();
        assertThat(context.selectedAnswer()).isNull();
        assertThat(context.rawClientData()).isNull();
        assertThat(other).isNotEqualTo(owner);
    }

    private AuthPrincipal principal(UUID id) {
        return new AuthPrincipal(id, "user@example.test", "User", UserRole.CUSTOMER);
    }

    private LearningRepository emptyLearning() {
        return new LearningRepository() {
            public void saveAttempt(LearningAttempt attempt) {}
            public List<LearningAttempt> findAttempts(UUID userId) { return List.of(); }
            public void saveActivity(LearningActivity activity) {}
            public List<LearningActivity> findActivities(UUID userId) { return List.of(); }
        };
    }

    private WritingRepository emptyWriting() {
        return new WritingRepository() {
            public void save(WritingAssessment assessment) {}
            public List<WritingAssessment> findByUser(UUID userId) { return List.of(); }
        };
    }

    private SpeakingRepository emptySpeaking() {
        return new SpeakingRepository() {
            public void save(SpeakingAttempt attempt) {}
            public List<SpeakingAttempt> findByUser(UUID userId) { return List.of(); }
        };
    }

    private PracticeAttemptStore emptyAttempts() {
        return new PracticeAttemptStore() {
            public void save(UUID userId, String skill, String setId, int score, int total,
                    java.util.Map<String, String> answers) {}
            public java.util.Optional<com.ieltsaitutor.practice.PracticeAttemptSnapshot> findLatest(
                    UUID userId, String skill, String setId) { return java.util.Optional.empty(); }
        };
    }
}
