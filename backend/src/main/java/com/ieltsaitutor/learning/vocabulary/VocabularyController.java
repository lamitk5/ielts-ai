package com.ieltsaitutor.learning.vocabulary;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/vocabulary")
public class VocabularyController {
    private final VocabularyService service;
    public VocabularyController(VocabularyService service) { this.service = service; }
    public record SaveRequest(String word, String meaning, String exampleSentence, String note, String source, String sourceReferenceId) {}
    public record ReviewRequest(VocabularyStatus status) {}
    @GetMapping public List<VocabularyItem> list(@RequestParam(required = false) String q, @RequestParam(required = false) VocabularyStatus status, HttpServletRequest request) { return service.list(user(request), q, status); }
    @PostMapping public VocabularyItem create(@RequestBody SaveRequest request, HttpServletRequest http) { try { return service.create(user(http), new VocabularyService.SaveCommand(request.word(), request.meaning(), request.exampleSentence(), request.note(), request.source(), request.sourceReferenceId())); } catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()); } }
    @PutMapping("/{id}") public VocabularyItem update(@PathVariable UUID id, @RequestBody SaveRequest request, HttpServletRequest http) { try { return service.update(user(http), id, new VocabularyService.SaveCommand(request.word(), request.meaning(), request.exampleSentence(), request.note(), request.source(), request.sourceReferenceId())); } catch (java.util.NoSuchElementException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage()); } }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id, HttpServletRequest http) { try { service.delete(user(http), id); } catch (java.util.NoSuchElementException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage()); } }
    @PostMapping("/{id}/review") public VocabularyItem review(@PathVariable UUID id, @RequestBody ReviewRequest request, HttpServletRequest http) { try { return service.review(user(http), id, request == null ? null : request.status()); } catch (java.util.NoSuchElementException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage()); } }
    private UUID user(HttpServletRequest request) { Object p = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE); if (p instanceof AuthPrincipal principal) return principal.userId(); throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để học từ vựng."); }
}
