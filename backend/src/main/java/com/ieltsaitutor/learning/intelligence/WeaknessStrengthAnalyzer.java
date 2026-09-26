package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WeaknessStrengthAnalyzer {
    public List<StudentLearningIssue> analyze(UUID userId, List<MistakeRecord> records, Instant asOf) {
        Map<String, List<MistakeRecord>> grouped = records == null ? Map.of() : records.stream()
                .filter(record -> record.userId().equals(userId) && record.status() != MistakeStatus.RESOLVED)
                .collect(Collectors.groupingBy(record -> record.skill().name() + ":" + record.category()));
        List<StudentLearningIssue> result = new ArrayList<>();
        grouped.forEach((key, items) -> {
            MistakeRecord first = items.get(0);
            long attempts = items.stream().map(MistakeRecord::attemptId).filter(java.util.Objects::nonNull).distinct().count();
            int occurrences = items.size();
            EvidenceState state = occurrences >= 3 && attempts >= 2 ? EvidenceState.CONFIRMED
                    : occurrences >= 2 ? EvidenceState.EMERGING : EvidenceState.OBSERVATION;
            double confidence = Math.min(1d, occurrences / 3d);
            UUID issueId = UUID.nameUUIDFromBytes((userId + ":WEAKNESS:" + key).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            result.add(new StudentLearningIssue(issueId, userId, IssueKind.WEAKNESS, first.skill(), first.category(), state,
                    state == EvidenceState.CONFIRMED ? IssueStatus.OPEN : IssueStatus.UNKNOWN, confidence, occurrences,
                    (int) attempts, items.stream().map(MistakeRecord::detectedAt).max(Instant::compareTo).orElse(null),
                    first.evidenceCode()));
        });
        return result;
    }
}
