package com.github.fiodarks.project26.users.application.port.in;

import java.util.Objects;

public record RegisterLocalUserCommand(
        String email,
        String name,
        String surname,
        String passwordHash
) {
    public RegisterLocalUserCommand {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (surname == null || surname.isBlank()) {
            throw new IllegalArgumentException("surname is required");
        }
        passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        if (passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
    }
}

