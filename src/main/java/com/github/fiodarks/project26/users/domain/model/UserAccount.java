package com.github.fiodarks.project26.users.domain.model;

import com.github.fiodarks.project26.archive.domain.model.UserId;
import com.github.fiodarks.project26.security.Role;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;

public record UserAccount(
        UserId id,
        String email,
        String name,
        String surname,
        String pictureUrl,
        String passwordHash,
        Set<Role> roles,
        OffsetDateTime blockedUntil,
        String blockedReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long version
) {
    public UserAccount {
        Objects.requireNonNull(id, "id");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public boolean isBlockedAt(OffsetDateTime at) {
        Objects.requireNonNull(at, "at");
        if (blockedUntil == null) {
            return false;
        }
        return at.isBefore(blockedUntil);
    }
}
