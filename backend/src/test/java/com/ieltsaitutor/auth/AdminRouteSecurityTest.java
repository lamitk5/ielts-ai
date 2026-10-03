package com.ieltsaitutor.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AdminRouteSecurityTest {
    @Test
    void everyAdminApiRouteRequiresAuthentication() throws Exception {
        AuthService service = mock(AuthService.class);
        AuthInterceptor interceptor = new AuthInterceptor(service);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/overview");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertEquals(false, interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }

    @Test
    void authenticatedCustomerCannotUseAdminApi() throws Exception {
        AuthService service = mock(AuthService.class);
        UUID userId = UUID.randomUUID();
        org.mockito.Mockito.when(service.authenticate("Bearer customer-token"))
                .thenReturn(new AuthUser(userId, "customer@example.com", "Customer", "hash", UserRole.CUSTOMER, java.time.Instant.now()));
        AuthInterceptor interceptor = new AuthInterceptor(service);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/overview");
        request.addHeader("Authorization", "Bearer customer-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertEquals(false, interceptor.preHandle(request, response, new Object()));
        assertEquals(403, response.getStatus());
    }

    @Test
    void legacyRagTokenRouteIsLeftForItsDedicatedInterceptor() throws Exception {
        AuthInterceptor interceptor = new AuthInterceptor(mock(AuthService.class));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/rag/documents");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertEquals(true, interceptor.preHandle(request, response, new Object()));
    }
}
