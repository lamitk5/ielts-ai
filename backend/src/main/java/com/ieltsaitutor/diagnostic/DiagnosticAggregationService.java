package com.ieltsaitutor.diagnostic;

import com.ieltsaitutor.learning.intelligence.Skill;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DiagnosticAggregationService {
    private final DiagnosticSessionService sessions;
    private final DiagnosticSectionRepository sections;

    public DiagnosticAggregationService(DiagnosticSessionService sessions, DiagnosticSectionRepository sections) { this.sessions = sessions; this.sections = sections; }

    public DiagnosticResult result(UUID userId, UUID sessionId) {
        DiagnosticSession session = sessions.get(userId, sessionId);
        List<DiagnosticSectionResult> values = sections.findOwnedBySession(userId, sessionId);
        boolean sufficient = values.size() == Skill.values().length && values.stream().allMatch(s -> s.state() == DiagnosticSectionState.READY);
        String state = sufficient ? DiagnosticState.COMPLETED.name() : "INSUFFICIENT_EVIDENCE";
        return new DiagnosticResult(session.id(), state, "Estimated starting profile", values, sufficient,
                "Hồ sơ khởi điểm chỉ là ước lượng từ dữ liệu luyện tập hiện có, không phải xếp lớp IELTS chính thức.");
    }
}
