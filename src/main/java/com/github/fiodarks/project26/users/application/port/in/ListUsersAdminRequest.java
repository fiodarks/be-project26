package com.github.fiodarks.project26.users.application.port.in;

import com.github.fiodarks.project26.security.Role;

public record ListUsersAdminRequest(
        int page,
        int size,
        String q,
        Role role,
        Boolean blocked
) {
    public ListUsersAdminRequest {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be >= 1");
        }
        if (q != null && q.isBlank()) {
            q = null;
        }
    }

    public static ListUsersAdminRequest of(
            int page,
            int size,
            String q,
            Role role,
            Boolean blocked
    ) {
        return new ListUsersAdminRequest(page, size, q, role, blocked);
    }
}
