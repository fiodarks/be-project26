package com.github.fiodarks.project26.users.application.port.in;

import com.github.fiodarks.project26.archive.domain.model.UserId;
import com.github.fiodarks.project26.security.Role;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;

public record UserAdminSummary(
        UserId userId,
        String email,
        String name,
        String surname,
        Set<Role> roles,
        OffsetDateTime blockedUntil,
        String blockedReason,
        OffsetDateTime createdAt,
        OffsetDateTime lastLoginAt,
        Long materialsCount,
        OffsetDateTime lastMaterialCreatedAt
) {
    public UserAdminSummary {
        Objects.requireNonNull(userId, "userId");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(lastLoginAt, "lastLoginAt");
    }
}
