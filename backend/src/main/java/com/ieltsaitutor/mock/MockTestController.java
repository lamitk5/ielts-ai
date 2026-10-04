package com.ieltsaitutor.mock;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.submission.CanonicalSubmissionService;
import com.ieltsaitutor.submission.SubmissionDraftSnapshot;

@RestController
@RequestMapping("/api/mock-tests/sessions")
public class MockTestController {

    private final MockTestService mockTestService;
    private final CanonicalSubmissionService submissionService;
    private final MockTestGradingService gradingService;

    public MockTestController(
            MockTestService mockTestService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) CanonicalSubmissionService submissionService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) MockTestGradingService gradingService) {
        this.mockTestService = mockTestService;
        this.submissionService = submissionService;
        this.gradingService = gradingService;
    }

    public record StartSessionRequest(String mockTestId) {}

    public record CommandRequest(String command) {}

    public record SectionDraftRequest(
            Map<String, String> answers,
            long expectedRevision,
            String idempotencyKey
    ) {}

    public record MockTestSectionDto(
            UUID id,
            UUID sessionId,
            int sectionOrder,
            String skill,
            String practiceId,
            String practiceVersionId,
            String publishedSetId,
            UUID submissionId,
            int timeLimitSeconds,
            String status
    ) {
        public static MockTestSectionDto from(MockTestSection s) {
            return new MockTestSectionDto(
                    s.id(), s.sessionId(), s.sectionOrder(), s.skill(), s.practiceId(),
                    s.practiceVersionId(), s.publishedSetId(), s.submissionId(),
                    s.timeLimitSeconds(), s.status().name()
            );
        }
    }

    public record MockTestSessionResponse(
            UUID id,
            UUID userId,
            String mockTestId,
            String mockTestVersion,
            String status,
            int currentSectionIndex,
            int totalTimeLimitSeconds,
            int elapsedSeconds,
            Instant startedAt,
            Instant expiresAt,
            Instant completedAt,
            List<MockTestSectionDto> sections
    ) {
        public static MockTestSessionResponse from(MockTestSession s) {
            return new MockTestSessionResponse(
                    s.id(), s.userId(), s.mockTestId(), s.mockTestVersion(), s.status().name(),
                    s.currentSectionIndex(), s.totalTimeLimitSeconds(), s.elapsedSeconds(),
                    s.startedAt(), s.expiresAt(), s.completedAt(),
                    s.sections().stream().map(MockTestSectionDto::from).toList()
            );
        }
    }

    @PostMapping("/start")
    public MockTestSessionResponse startOrResume(
            @RequestBody(required = false) StartSessionRequest request,
            HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        String testId = request != null ? request.mockTestId() : null;
        MockTestSession session = mockTestService.startOrResume(principal.userId(), testId);
        return MockTestSessionResponse.from(session);
    }

    @GetMapping("/active")
    public MockTestSessionResponse getActiveSession(HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        return mockTestService.listUserSessions(principal.userId()).stream()
                .filter(s -> s.status().isMutable())
                .findFirst()
                .map(MockTestSessionResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có phiên thi thử nào đang hoạt động"));
    }

    @GetMapping("/{sessionId}")
    public MockTestSessionResponse getSession(
            @PathVariable UUID sessionId,
            HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        try {
            MockTestSession session = mockTestService.getSession(principal.userId(), sessionId);
            return MockTestSessionResponse.from(session);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/{sessionId}/command")
    public MockTestSessionResponse executeCommand(
            @PathVariable UUID sessionId,
            @RequestBody CommandRequest request,
            HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        if (request == null || request.command() == null || request.command().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Command is required");
        }
        try {
            MockTestCommand cmd = MockTestCommand.valueOf(request.command().trim().toUpperCase());
            MockTestSession session = mockTestService.executeCommand(principal.userId(), sessionId, cmd);
            return MockTestSessionResponse.from(session);
        } catch (MockConflictException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/{sessionId}/sections/{sectionIndex}/draft")
    public SubmissionDraftSnapshot autosaveSectionDraft(
            @PathVariable UUID sessionId,
            @PathVariable int sectionIndex,
            @RequestBody SectionDraftRequest request,
            HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        MockTestSession session = mockTestService.getSession(principal.userId(), sessionId);

        if (sectionIndex < 0 || sectionIndex >= session.sections().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section index out of bounds");
        }

        MockTestSection section = session.sections().get(sectionIndex);
        if (section.submissionId() == null || submissionService == null) {
            return new SubmissionDraftSnapshot(
                    section.submissionId() != null ? section.submissionId() : UUID.randomUUID(),
                    principal.userId(),
                    request.answers() != null ? request.answers() : Map.of(),
                    request.expectedRevision() + 1,
                    request.idempotencyKey() != null ? request.idempotencyKey() : "draft-" + UUID.randomUUID(),
                    Instant.now()
            );
        }

        return submissionService.autosave(
                principal.userId(),
                section.submissionId(),
                request.answers(),
                request.expectedRevision(),
                request.idempotencyKey() != null ? request.idempotencyKey() : "draft-" + UUID.randomUUID()
        );
    }

    @GetMapping("/{sessionId}/result")
    public MockTestResult getResult(
            @PathVariable UUID sessionId,
            HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        if (gradingService == null) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Grading service is not available");
        }
        try {
            return gradingService.getResult(principal.userId(), sessionId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping
    public List<MockTestSessionResponse> listSessions(HttpServletRequest httpRequest) {
        AuthPrincipal principal = principal(httpRequest);
        return mockTestService.listUserSessions(principal.userId()).stream()
                .map(MockTestSessionResponse::from)
                .toList();
    }

    private AuthPrincipal principal(HttpServletRequest request) {
        Object principal = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (principal instanceof AuthPrincipal authenticated) return authenticated;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }
}
