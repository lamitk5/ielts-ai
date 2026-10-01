package com.ieltsaitutor.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {
    private final AuthService service = mock(AuthService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new AuthExceptionHandler())
                .build();
    }

    @Test
    void registerReturnsNormalizedUserAndOpaqueSessionToken() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.register(any(RegisterCommand.class)))
                .thenReturn(new AuthSessionResponse("opaque-token", new AuthUserView(id, "student@example.com", "Mai", UserRole.CUSTOMER)));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Student@Example.com\",\"password\":\"password-123\",\"firstName\":\"Mai\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("opaque-token"))
                .andExpect(jsonPath("$.user.email").value("student@example.com"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    @Test
    void invalidRegistrationUsesSafeValidationPayload() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\",\"firstName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_REQUEST"));
    }

    @Test
    void meReturnsUnauthorizedErrorFromService() throws Exception {
        when(service.me(any())).thenThrow(new AuthException("AUTH_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục."));

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer expired"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_UNAUTHORIZED"));
    }
}
