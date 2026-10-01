package com.ieltsaitutor.speaking;

import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.learning.LearningRepository;
import com.ieltsaitutor.practice.PracticeAttemptStore;
import com.ieltsaitutor.practice.SyntheticPracticeCatalog;
import com.ieltsaitutor.tutor.context.DefaultTutorContextService;
import com.ieltsaitutor.tutor.context.TutorContextRequest;
import com.ieltsaitutor.tutor.context.TutorLearningContext;
import com.ieltsaitutor.writing.WritingRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SpeakingTutorContextTest {
    @Test
    void speakingContextUsesOwnedTextAttemptReference() {
        UUID userId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        SpeakingRepository repository = mock(SpeakingRepository.class);
        when(repository.findByUserAndId(userId, attemptId)).thenReturn(java.util.Optional.of(
                new SpeakingAttempt(attemptId, userId, "speaking-p2-01", "I study at the library.", null, "INPUT_SAVED", null,
                        java.time.Instant.now())));
        DefaultTutorContextService service = new DefaultTutorContextService(
                SyntheticPracticeCatalog.inMemory(), mock(LearningRepository.class), mock(WritingRepository.class),
                repository, mock(PracticeAttemptStore.class));

        TutorLearningContext context = service.resolve(
                new AuthPrincipal(userId, "student@test", "Student", UserRole.CUSTOMER),
                new TutorContextRequest("speaking", null, null, attemptId, null, "speaking-p2-01"));

        assertThat(context.available()).isTrue();
        assertThat(context.speakingTranscript()).isEqualTo("I study at the library.");
        verify(repository).findByUserAndId(userId, attemptId);
    }
}
