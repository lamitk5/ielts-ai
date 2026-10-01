package com.ieltsaitutor.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/register")
    public AuthSessionResponse register(@Valid @RequestBody RegisterCommand command) { return service.register(command); }

    @PostMapping("/login")
    public AuthSessionResponse login(@Valid @RequestBody LoginCommand command) { return service.login(command); }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        service.logout(authorization);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthSessionResponse me(HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthPrincipal principal = (AuthPrincipal) request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (principal != null) return new AuthSessionResponse(null,
                new AuthUserView(principal.userId(), principal.email(), principal.firstName(), principal.role()));
        return service.me(authorization);
    }
}
