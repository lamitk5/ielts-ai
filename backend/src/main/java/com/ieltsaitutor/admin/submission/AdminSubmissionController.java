package com.ieltsaitutor.admin.submission;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.results.LearnerResult;
import com.ieltsaitutor.submission.SubmissionHistoryPage;

@RestController
@RequestMapping("/api/admin/submissions")
public class AdminSubmissionController {
    private final AdminSubmissionQueryService service;

    public AdminSubmissionController(AdminSubmissionQueryService service) { this.service = service; }

    @GetMapping
    public QueueResponse list(@RequestParam(required = false) String skill, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest request) {
        try {
            SubmissionHistoryPage result = service.list(principal(request), skill, status, page, Math.min(size, 100));
            return new QueueResponse(result.items().stream().map(AdminSubmissionView::from).toList(), result.page(), result.size(), result.total());
        } catch (SecurityException ex) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage()); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()); }
    }

    @GetMapping("/{submissionId}/result")
    public LearnerResult result(@PathVariable UUID submissionId, HttpServletRequest request) {
        try { return service.result(principal(request), submissionId); }
        catch (SecurityException ex) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage()); }
        catch (RuntimeException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission không tồn tại."); }
    }

    private AuthPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (value instanceof AuthPrincipal principal) return principal;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }

    public record QueueResponse(List<AdminSubmissionView> items, int page, int size, long total) {}
}
