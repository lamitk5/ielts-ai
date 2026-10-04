package com.ieltsaitutor.learning.notebook;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/me/error-notebook")
public class ErrorNotebookController {
    private final ErrorNotebookService service;
    private final ErrorNotebookActionService actions;
    public ErrorNotebookController(ErrorNotebookService service, ErrorNotebookActionService actions) { this.service = service; this.actions = actions; }
    @GetMapping
    public ErrorNotebookService.NotebookResponse list(@RequestParam(required = false) String skill, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestAttribute(value = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để xem sổ lỗi sai.");
        try { return service.list(principal.userId(), new ErrorNotebookQuery(skill, status, page, size)); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); }
    }
    @PostMapping("/{mistakeId}/acknowledge")
    public AckResponse acknowledge(@PathVariable UUID mistakeId, @RequestAttribute(value = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
        actions.acknowledge(principal.userId(), mistakeId);
        return new AckResponse(true);
    }
    public record AckResponse(boolean acknowledged) {}
}
