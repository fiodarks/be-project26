package com.github.fiodarks.project26.users.application.port.out;

import com.github.fiodarks.project26.security.Role;

import java.time.OffsetDateTime;
import java.util.Objects;

public record UserAccountSearchCriteria(
        int page,
        int size,
        String q,
        Role role,
        Boolean blocked,
        OffsetDateTime now
) {
    public UserAccountSearchCriteria {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be >= 1");
        }
        if (q != null && q.isBlank()) {
            q = null;
        }
        Objects.requireNonNull(now, "now");
    }
}

