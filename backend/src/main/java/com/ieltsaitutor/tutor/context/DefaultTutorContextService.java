package com.ieltsaitutor.tutor.context;

import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.learning.LearningAttempt;
import com.ieltsaitutor.learning.LearningRepository;
import com.ieltsaitutor.practice.PracticeQuestion;
import com.ieltsaitutor.practice.PracticeSet;
import com.ieltsaitutor.practice.SyntheticPracticeCatalog;
import com.ieltsaitutor.practice.PracticeAttemptStore;
import com.ieltsaitutor.practice.PracticeAttemptSnapshot;
import com.ieltsaitutor.speaking.SpeakingAttempt;
import com.ieltsaitutor.speaking.SpeakingRepository;
import com.ieltsaitutor.writing.WritingAssessment;
import com.ieltsaitutor.writing.WritingRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;

@Service
public class DefaultTutorContextService implements TutorContextService {
    private final SyntheticPracticeCatalog catalog;
    private final LearningRepository learning;
    private final WritingRepository writing;
    private final SpeakingRepository speaking;
    private final PracticeAttemptStore attempts;

    @Autowired
    public DefaultTutorContextService(SyntheticPracticeCatalog catalog, LearningRepository learning,
            WritingRepository writing, SpeakingRepository speaking) {
        this(catalog, learning, writing, speaking, null);
    }

    public DefaultTutorContextService(SyntheticPracticeCatalog catalog, LearningRepository learning,
            WritingRepository writing, SpeakingRepository speaking, PracticeAttemptStore attempts) {
        this.catalog = catalog;
        this.learning = learning;
        this.writing = writing;
        this.speaking = speaking;
        this.attempts = attempts;
    }

    @Override
    public TutorLearningContext resolve(AuthPrincipal principal, TutorContextRequest request) {
        if (request == null) throw new TutorContextException("TUTOR_CONTEXT_INVALID", 400, "Context Tutor không hợp lệ.");
        if (principal == null && privateReference(request)) {
            throw new TutorContextException("TUTOR_CONTEXT_UNAUTHORIZED", 401, "Đăng nhập để xem ngữ cảnh cá nhân.");
        }
        if (isPractice(request)) return practice(principal, request);
        if ("writing".equals(request.skill())) return writing(principal, request);
        if ("speaking".equals(request.skill())) return speaking(principal, request);
        if (principal != null && noReference(request)) return progress(principal);
        return TutorLearningContext.absent(request.skill());
    }

    private TutorLearningContext practice(AuthPrincipal principal, TutorContextRequest request) {
        try {
            PracticeSet set = catalog.find(request.skill(), request.setId());
            PracticeQuestion question = set.questions().stream()
                    .filter(candidate -> Objects.equals(candidate.id(), request.questionId())).findFirst().orElse(null);
            if (question == null) return TutorLearningContext.absent(request.skill());
            String text = set.title() + "\n" + question.prompt();
            PracticeAttemptSnapshot attempt = principal == null || attempts == null ? null
                    : attempts.findLatest(principal.userId(), request.skill(), request.setId()).orElse(null);
            String selected = attempt == null ? null : attempt.answers().get(question.id());
            return new TutorLearningContext(true, request.skill(), text, question.id(), question.prompt(), selected,
                    principal != null ? question.answerKey() : null, principal != null ? question.explanation() : null,
                    attempt == null ? null : attempt.score(), attempt == null ? null : attempt.total(),
                    null, null, null, null, List.of(), null);
        } catch (IllegalArgumentException exception) {
            return TutorLearningContext.absent(request.skill());
        }
    }

    private TutorLearningContext writing(AuthPrincipal principal, TutorContextRequest request) {
        if (principal == null) return TutorLearningContext.absent(request.skill());
        WritingAssessment item = writing.findByUser(principal.userId()).stream()
                .filter(candidate -> Objects.equals(candidate.taskId(), request.taskId())).findFirst().orElse(null);
        if (item == null) return TutorLearningContext.absent(request.skill());
        return new TutorLearningContext(true, "writing", "Writing task " + item.taskId(), null, null, null, null,
                null, null, null, item.taskId(), item.submittedText(), null, null,
                item.issues() == null ? List.of() : item.issues(), null);
    }

    private TutorLearningContext speaking(AuthPrincipal principal, TutorContextRequest request) {
        if (principal == null) return TutorLearningContext.absent(request.skill());
        SpeakingAttempt item = speaking.findByUser(principal.userId()).stream()
                .filter(candidate -> Objects.equals(candidate.promptId(), request.promptId())).findFirst().orElse(null);
        if (item == null) return TutorLearningContext.absent(request.skill());
        return new TutorLearningContext(true, "speaking", "Speaking prompt " + item.promptId(), null, null, null, null,
                null, null, null, null, null, item.promptId(), item.transcript(), List.of(), null);
    }

    private TutorLearningContext progress(AuthPrincipal principal) {
        List<LearningAttempt> attempts = learning.findAttempts(principal.userId()).stream().limit(20).toList();
        String summary = attempts.stream().map(item -> item.skill() + " " + item.score() + "/" + item.total())
                .reduce((left, right) -> left + "; " + right).orElse("Chưa có lượt luyện tập.");
        return new TutorLearningContext(true, "", summary, null, null, null, null, null, null, null,
                null, null, null, null, List.of(), null);
    }

    private boolean isPractice(TutorContextRequest request) {
        return ("reading".equals(request.skill()) || "listening".equals(request.skill()))
                && request.setId() != null && request.questionId() != null;
    }

    private boolean privateReference(TutorContextRequest request) {
        return request.attemptId() != null || request.taskId() != null || request.promptId() != null;
    }

    private boolean noReference(TutorContextRequest request) {
        return request.attemptId() == null && request.setId() == null && request.questionId() == null
                && request.taskId() == null && request.promptId() == null;
    }
}
