package com.ieltsaitutor.profile;

import java.util.UUID;

public interface LearnerProfileRepository {
    void updateDisplayNameAndAvatar(UUID userId, String firstName, String avatarUrl);

    void updatePasswordHash(UUID userId, String passwordHash);

    String getAvatarUrl(UUID userId);
}
