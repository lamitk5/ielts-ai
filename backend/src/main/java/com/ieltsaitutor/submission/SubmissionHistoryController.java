package com.ieltsaitutor.submission;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/me/submissions")
public class SubmissionHistoryController {
    private final SubmissionHistoryService service;

    public SubmissionHistoryController(SubmissionHistoryService service) {
        this.service = service;
    }

    @GetMapping
    public HistoryResponse list(@RequestParam(required = false) String skill,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        AuthPrincipal principal = principal(request);
        SubmissionHistoryPage result = service.list(principal.userId(), skill, status, page, size);
        return new HistoryResponse(result.items().stream().map(SubmissionHistoryView::from).toList(),
                result.page(), result.size(), result.total());
    }

    private AuthPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (value instanceof AuthPrincipal principal) return principal;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }

    public record HistoryResponse(List<SubmissionHistoryView> items, int page, int size, long total) {}
}
