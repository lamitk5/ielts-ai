package com.ieltsaitutor.admin.submission;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.results.LearnerResult;
import com.ieltsaitutor.results.LearnerResultService;
import com.ieltsaitutor.submission.AdminSubmissionQuery;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import com.ieltsaitutor.submission.SubmissionHistoryPage;
import com.ieltsaitutor.submission.SubmissionStatus;

@Service
public class AdminSubmissionQueryService {
    private static final Set<String> SKILLS = Set.of("reading", "listening", "writing", "speaking");
    private final PracticeSubmissionRepository submissions;
    private final LearnerResultService results;

    public AdminSubmissionQueryService(PracticeSubmissionRepository submissions, LearnerResultService results) {
        this.submissions = submissions;
        this.results = results;
    }

    public SubmissionHistoryPage list(AuthPrincipal principal, String skill, String status, int page, int size) {
        requireAdmin(principal);
        String normalizedSkill = skill == null || skill.isBlank() ? null : skill.trim().toLowerCase(Locale.ROOT);
        if (normalizedSkill != null && !SKILLS.contains(normalizedSkill)) throw new IllegalArgumentException("Invalid skill filter");
        SubmissionStatus normalizedStatus = null;
        if (status != null && !status.isBlank()) normalizedStatus = SubmissionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        return submissions.findAllHistory(new AdminSubmissionQuery(normalizedSkill, normalizedStatus, page, Math.min(size, 100)));
    }

    public LearnerResult result(AuthPrincipal principal, UUID submissionId) {
        requireAdmin(principal);
        return results.getForAdmin(submissionId);
    }

    private void requireAdmin(AuthPrincipal principal) {
        if (principal == null || principal.role() != UserRole.ADMIN) throw new SecurityException("Quyền quản trị viên là bắt buộc.");
    }
}
