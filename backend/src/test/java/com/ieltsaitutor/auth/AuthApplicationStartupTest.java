package com.ieltsaitutor.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AuthApplicationStartupTest {
    @Autowired
    private AuthService authService;

    @Test
    void fullApplicationContextStartsWithAuthService() {
        assertThat(authService).isNotNull();
    }
}
