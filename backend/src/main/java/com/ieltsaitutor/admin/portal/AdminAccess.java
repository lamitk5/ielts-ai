package com.ieltsaitutor.admin.portal;

import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;

final class AdminAccess {
    private AdminAccess() {}

    static void requireAdmin(AuthPrincipal principal) {
        if (principal == null || principal.role() != UserRole.ADMIN) {
            throw new SecurityException("Quyền quản trị viên là bắt buộc.");
        }
    }
}
