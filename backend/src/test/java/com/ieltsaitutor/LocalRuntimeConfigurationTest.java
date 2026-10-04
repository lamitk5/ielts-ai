package com.ieltsaitutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class LocalRuntimeConfigurationTest {
    @Test
    void defaultBackendPortMatchesTheFrontendLocalProxy() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
            assertThat(input).as("application.properties should be available on the test classpath").isNotNull();
            properties.load(input);
        }

        assertThat(properties.getProperty("server.port")).isEqualTo("${PORT:8081}");
    }
}
