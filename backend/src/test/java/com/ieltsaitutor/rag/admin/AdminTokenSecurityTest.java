package com.ieltsaitutor.rag.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;

class AdminTokenSecurityTest {
    @Test
    void missingTokenReturns401() throws Exception { mvc("secret").perform(get("/api/admin/rag/protected")).andExpect(status().isUnauthorized()); }

    @Test
    void missingConfiguredTokenReturns401() throws Exception { mvc("").perform(get("/api/admin/rag/protected")).andExpect(status().isUnauthorized()); }

    @Test
    void wrongTokenReturns403() throws Exception {
        mvc("secret").perform(get("/api/admin/rag/protected").header("X-Admin-Token", "wrong"))
                .andExpect(status().isForbidden());
    }

    @Test
    void correctTokenAuthorizes() throws Exception {
        mvc("secret").perform(get("/api/admin/rag/protected").header("X-Admin-Token", "secret"))
                .andExpect(status().isOk()).andExpect(content().string("ok"));
    }

    @Test
    void nonAdminRouteDoesNotRequireToken() throws Exception {
        mvc("secret").perform(get("/public")).andExpect(status().isOk());
    }

    @Test
    void tokenDoesNotAppearInErrorBody() throws Exception {
        mvc("secret").perform(get("/api/admin/rag/protected").header("X-Admin-Token", "wrong"))
                .andExpect(status().isForbidden()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }

    @Test
    void authenticatedAdminRoleAuthorizesWithoutCompatibilityToken() throws Exception {
        MockMvc secured = MockMvcBuilders.standaloneSetup(new ProtectedController())
                .addInterceptors(new AdminTokenInterceptor(new AdminTokenAuthorizationService("")))
                .build();

        secured.perform(get("/api/admin/rag/protected")
                        .requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                                new AuthPrincipal(java.util.UUID.randomUUID(), "admin@example.com", "Admin", UserRole.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedCustomerRoleCannotAuthorizeAdminRoute() throws Exception {
        MockMvc secured = MockMvcBuilders.standaloneSetup(new ProtectedController())
                .addInterceptors(new AdminTokenInterceptor(new AdminTokenAuthorizationService("")))
                .build();

        secured.perform(get("/api/admin/rag/protected")
                        .requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                                new AuthPrincipal(java.util.UUID.randomUUID(), "student@example.com", "Student", UserRole.CUSTOMER)))
                .andExpect(status().isUnauthorized());
    }

    private MockMvc mvc(String configuredToken) {
        AdminTokenInterceptor interceptor = new AdminTokenInterceptor(new AdminTokenAuthorizationService(configuredToken));
        return MockMvcBuilders.standaloneSetup(new ProtectedController()).addInterceptors(interceptor).build();
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/api/admin/rag/protected") String protectedEndpoint() { return "ok"; }
        @GetMapping("/public") String publicEndpoint() { return "ok"; }
    }
}
