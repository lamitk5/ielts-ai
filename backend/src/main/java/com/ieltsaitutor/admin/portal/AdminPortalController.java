package com.ieltsaitutor.admin.portal;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/admin")
public class AdminPortalController {
    private final AdminPortalService service;
    public AdminPortalController(AdminPortalService service) { this.service = service; }

    @GetMapping("/overview") public Map<String,Object> overview(HttpServletRequest request) { return invoke(request, service::overview); }
    @GetMapping("/learners") public AdminPortalService.Page learners(@RequestParam(defaultValue="") String query,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest req) { return service.learners(principal(req),query,page,size); }
    @GetMapping("/practices") public List<Map<String,Object>> practices(@RequestParam(defaultValue="") String state,@RequestParam(defaultValue="") String skill,HttpServletRequest req) { return service.practices(principal(req),state,skill); }
    @GetMapping("/reviews") public List<Map<String,Object>> reviews(HttpServletRequest req) { return service.reports(principal(req)); }
    @PostMapping("/reports/{id}/resolve") public void resolve(@PathVariable UUID id,HttpServletRequest req) { service.resolveReport(principal(req),id); }
    @GetMapping("/usage") public List<Map<String,Object>> usage(@RequestParam(defaultValue="") String provider,@RequestParam(defaultValue="") String feature,HttpServletRequest req) { return service.usage(principal(req),provider,feature); }
    @GetMapping("/audit") public AdminPortalService.Page audit(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest req) { return service.audit(principal(req),page,size); }
    @GetMapping("/prompts") public List<Map<String,Object>> prompts(HttpServletRequest req) { return service.prompts(principal(req)); }
    public record DraftRequest(String promptKey,String name,String purpose,String content) {}
    @PostMapping("/prompts/drafts") public Map<String,Object> draft(@RequestBody DraftRequest body,HttpServletRequest req) { return service.createDraft(principal(req),body.promptKey(),body.name(),body.purpose(),body.content()); }
    @PostMapping("/prompts/versions/{id}/activate") public void activate(@PathVariable UUID id,HttpServletRequest req) { service.activatePrompt(principal(req),id); }

    private AuthPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (value instanceof AuthPrincipal principal) return principal;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục.");
    }
    private <T> T invoke(HttpServletRequest request, java.util.function.Function<AuthPrincipal,T> fn) { return fn.apply(principal(request)); }
}
