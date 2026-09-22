package com.ieltsaitutor.search;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/practice/search")
public class PracticeSearchController {
    private final PracticeSearchService service;
    public PracticeSearchController(PracticeSearchService service) { this.service = service; }

    @GetMapping
    public List<PracticeSearchResult> search(@RequestParam String q) {
        try { return service.search(q); }
        catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Từ khóa tìm kiếm chưa hợp lệ."); }
    }
}
