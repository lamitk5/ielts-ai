package com.ieltsaitutor.profile;

public record ChangePasswordCommand(String currentPassword, String newPassword) {
    public ChangePasswordCommand {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không được để trống.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu mới không được để trống.");
        }
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 8 ký tự.");
        }
    }
}
