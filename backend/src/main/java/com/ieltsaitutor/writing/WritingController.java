package com.ieltsaitutor.writing;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/practice/writing")
public class WritingController {
    private final WritingAssessmentService service;
    private final WritingRepository repository;

    public WritingController(WritingAssessmentService service, WritingRepository repository) {
        this.service = service; this.repository = repository;
    }

    @GetMapping("/tasks")
    public List<WritingTask> tasks() { return List.of(
            new WritingTask("task-1-academic-01", "TASK_1", "Academic Task 1", "Summarise the information in a chart or process.", 150),
            new WritingTask("task-2-opinion-01", "TASK_2", "Essay Task 2", "Discuss both views and give your own opinion.", 250)); }

    @PostMapping("/submissions")
    public WritingAssessment submit(@RequestBody SubmissionRequest request, HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        if (request.responseText() == null || request.responseText().trim().split("\\s+").length < 20)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bài viết cần thêm nội dung để đánh giá.");
        return service.assess(principal.userId(), request.taskId(), request.responseText());
    }

    @GetMapping("/submissions")
    public List<WritingAssessment> submissions(HttpServletRequest request) { return repository.findByUser(principal(request).userId()); }

    private AuthPrincipal principal(HttpServletRequest request) {
        Object principal = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (principal instanceof AuthPrincipal authenticated) return authenticated;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }

    public record SubmissionRequest(String taskId, String responseText) {}
}
