package com.ieltsaitutor.submission;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class SubmissionFinalizationService {
    private final PracticeSubmissionRepository submissions;
    private final SubmissionAnswerRepository answers;
    private final Supplier<Instant> clock;

    @Autowired
    public SubmissionFinalizationService(PracticeSubmissionRepository submissions, SubmissionAnswerRepository answers) {
        this(submissions, answers, Instant::now);
    }

    public SubmissionFinalizationService(PracticeSubmissionRepository submissions, SubmissionAnswerRepository answers,
            Supplier<Instant> clock) {
        this.submissions = submissions;
        this.answers = answers;
        this.clock = clock;
    }

    @Transactional
    public PracticeSubmission finalizeSubmission(UUID ownerId, UUID submissionId, Map<String, String> answerPayload,
            String submitIdempotencyKey) {
        if (submitIdempotencyKey == null || submitIdempotencyKey.isBlank()) {
            throw new SubmissionConflictException("Submission idempotency key is required");
        }
        Map<String, String> normalizedAnswers = answerPayload == null ? Map.of() : Map.copyOf(answerPayload);
        String normalizedKey = submitIdempotencyKey.trim();
        String hash = contentHash(normalizedAnswers);
        PracticeSubmission current = submissions.findByOwnerAndId(ownerId, submissionId)
                .orElseThrow(() -> new SubmissionConflictException("Submission is not available"));

        if (!current.editable()) {
            if ((current.status() == SubmissionStatus.SUBMITTED || current.status() == SubmissionStatus.GRADED)
                    && Objects.equals(current.submitIdempotencyKey(), normalizedKey)
                    && Objects.equals(current.contentHash(), hash)) return current;
            throw new SubmissionConflictException("Submission is already finalized");
        }

        Instant submittedAt = clock.get();
        PracticeSubmission finalized = submissions.finalizeIfEditable(submissionId, normalizedKey, hash, submittedAt)
                .orElseGet(() -> submissions.findByOwnerAndId(ownerId, submissionId)
                        .filter(item -> item.status() == SubmissionStatus.SUBMITTED
                                && Objects.equals(item.submitIdempotencyKey(), normalizedKey)
                                && Objects.equals(item.contentHash(), hash))
                        .orElseThrow(() -> new SubmissionConflictException("Submission finalization conflict")));
        answers.save(new SubmissionAnswerSnapshot(submissionId, ownerId, normalizedAnswers, hash, submittedAt));
        return finalized;
    }

    public static String contentHash(Map<String, String> answers) {
        String canonical = (answers == null ? Map.<String, String>of() : answers).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> escape(entry.getKey()) + "=" + escape(entry.getValue()))
                .collect(Collectors.joining("\n"));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\n", "\\n").replace("=", "\\=");
    }
}
