package com.ieltsaitutor.diagnostic;

import com.ieltsaitutor.assessment.objective.QuestionResultRepository;
import com.ieltsaitutor.learning.intelligence.Skill;
import com.ieltsaitutor.results.LearnerResult;
import com.ieltsaitutor.results.LearnerResultService;
import com.ieltsaitutor.auth.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class DiagnosticSubmissionService {
    private final DiagnosticSessionService sessions;
    private final DiagnosticSectionRepository sections;
    private final LearnerResultService results;

    public DiagnosticSubmissionService(DiagnosticSessionService sessions, DiagnosticSectionRepository sections, LearnerResultService results) {
        this.sessions = sessions; this.sections = sections; this.results = results;
    }

    public DiagnosticSectionResult recordCanonicalResult(UUID userId, UUID sessionId, Skill skill, UUID submissionId) {
        DiagnosticSession session = sessions.get(userId, sessionId);
        if (session.state() != DiagnosticState.IN_PROGRESS) throw invalid("Diagnostic session is not active");
        LearnerResult result = results.get(userId, submissionId);
        if (!skill.name().equalsIgnoreCase(result.skill())) throw invalid("Result skill does not match diagnostic section");
        Double evaluatedBand = result.estimatedBand();
        if (evaluatedBand == null && result.humanReview() != null) evaluatedBand = result.humanReview().overallBand();
        boolean hasObjectiveScore = result.score() != null && result.total() != null;
        if (!hasObjectiveScore && evaluatedBand == null) {
            return sections.save(DiagnosticSectionResult.insufficient(sessionId, userId, skill, "Phần này chưa có kết quả đủ tin cậy.", Instant.now()));
        }
        return sections.save(new DiagnosticSectionResult(UUID.randomUUID(), sessionId, userId, skill,
                DiagnosticSectionState.READY, result.score(), result.total(), evaluatedBand,
                result.accuracy() != null && result.accuracy().doubleValue() >= 70 ? DiagnosticConfidence.MEDIUM : DiagnosticConfidence.LOW,
                submissionId, "submission:" + submissionId, null, Instant.now()));
    }

    public DiagnosticSectionResult recordUnavailable(UUID userId, UUID sessionId, Skill skill, String message) {
        sessions.get(userId, sessionId);
        return sections.save(DiagnosticSectionResult.insufficient(sessionId, userId, skill, message, Instant.now()));
    }

    private AuthException invalid(String message) { return new AuthException("DIAGNOSTIC_INVALID", HttpStatus.BAD_REQUEST, message); }
}
