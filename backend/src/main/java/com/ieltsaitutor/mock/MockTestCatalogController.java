package com.ieltsaitutor.mock;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
public class MockTestCatalogController {
    private final MockTestCatalogService service;
    public MockTestCatalogController(MockTestCatalogService service) { this.service = service; }
    @GetMapping("/api/mock-tests") public List<MockTestCatalogItem> published() { return service.published(); }
    @GetMapping("/api/admin/mock-tests") public List<MockTestCatalogItem> all(HttpServletRequest request) { admin(request); return service.all(); }
    @PostMapping("/api/admin/mock-tests") public MockTestCatalogItem create(@RequestBody MockTestCatalogService.Command command, HttpServletRequest request) { admin(request); try { return service.save(command); } catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()); } }
    @PutMapping("/api/admin/mock-tests/{id}") public MockTestCatalogItem update(@PathVariable UUID id, @RequestBody MockTestCatalogService.Command command, HttpServletRequest request) { admin(request); try { return service.update(id, command); } catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()); } catch (java.util.NoSuchElementException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage()); } }
    @PostMapping("/api/admin/mock-tests/{id}/publish") public MockTestCatalogItem publish(@PathVariable UUID id, @RequestParam boolean value, HttpServletRequest request) { admin(request); try { return service.publish(id, value); } catch (java.util.NoSuchElementException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage()); } }
    @DeleteMapping("/api/admin/mock-tests/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id, HttpServletRequest request) { admin(request); service.delete(id); }
    private void admin(HttpServletRequest request) { Object p = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE); if (!(p instanceof AuthPrincipal principal) || principal.role() != com.ieltsaitutor.auth.UserRole.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Quyền quản trị viên là bắt buộc."); }
}
