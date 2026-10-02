package com.ieltsaitutor.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthInterceptorSubmissionRouteTest {
    @Test
    void canonicalSubmissionRoutesRejectMissingAuthenticationBeforeController() throws Exception {
        AuthInterceptor interceptor = new AuthInterceptor(mock(AuthService.class));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/submissions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }
}
