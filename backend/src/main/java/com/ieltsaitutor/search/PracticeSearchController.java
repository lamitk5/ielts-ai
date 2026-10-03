package com.ieltsaitutor.search;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/practice/search")
public class PracticeSearchController {
    private final PracticeSearchService service;

    public PracticeSearchController(PracticeSearchService service) {
        this.service = service;
    }

    @GetMapping
    public List<PracticeSearchResult> search(
            @RequestParam String q,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        try {
            UUID userId = null;
            Object principalObj = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
            if (principalObj instanceof AuthPrincipal principal) {
                userId = principal.userId();
            }
            return service.search(q, userId, skill, type, page, size);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Từ khóa tìm kiếm chưa hợp lệ.");
        }
    }
}
