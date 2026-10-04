package com.ielts.tutor;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModuleVerificationTest {

    @Test
    void verifyModularStructure() {
        ApplicationModules.of(IeltsTutorApplication.class).verify();
    }
}
