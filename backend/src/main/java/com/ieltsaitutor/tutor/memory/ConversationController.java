package com.ieltsaitutor.tutor.memory;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/ai/conversations")
public class ConversationController {
    private final ConversationService service;
    public ConversationController(ConversationService service) { this.service = service; }

    @GetMapping public List<AiConversation> list(@RequestAttribute(value = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal) { return service.listOwned(user(principal)); }
    @GetMapping("/{id}") public ConversationDetail detail(@PathVariable UUID id, @RequestAttribute(value = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal) {
        UUID userId = user(principal);
        AiConversation conversation = service.findOwned(userId, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "conversation not found"));
        return new ConversationDetail(conversation, service.messages(userId, id));
    }
    @PostMapping("/{id}/archive") public ArchiveResponse archive(@PathVariable UUID id, @RequestAttribute(value = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal) {
        if (!service.archive(user(principal), id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "conversation not found");
        return new ArchiveResponse(true);
    }
    private UUID user(AuthPrincipal principal) { if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để xem hội thoại."); return principal.userId(); }
    public record ConversationDetail(AiConversation conversation, List<AiMessage> messages) {}
    public record ArchiveResponse(boolean archived) {}
}
