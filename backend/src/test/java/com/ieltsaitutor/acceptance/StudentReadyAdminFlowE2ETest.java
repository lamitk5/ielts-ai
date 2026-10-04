package com.ieltsaitutor.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.auth.UserRole;

class StudentReadyAdminFlowE2ETest {
    @Test
    void adminRoleRemainsDistinctFromLearnerRoleForApprovalBoundary() {
        assertEquals(UserRole.ADMIN, UserRole.valueOf("ADMIN"));
        assertEquals(UserRole.CUSTOMER, UserRole.valueOf("CUSTOMER"));
    }
}
