package com.github.fiodarks.project26.security;

import com.github.fiodarks.project26.archive.domain.model.UserId;

import java.util.Objects;
import java.util.Set;

public record Actor(UserId userId, Set<Role> roles) {
    public Actor {
        Objects.requireNonNull(userId, "userId");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
    }

    public boolean has(Role role) {
        return roles.contains(role);
    }
}

