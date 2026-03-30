package com.github.fiodarks.project26.users.application.port.in;

public record UserProfile(String email, String name, String surname, String pictureUrl) {
    public UserProfile {
        // allow nulls (claims can be missing) but forbid blank strings
        if (email != null && email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (surname != null && surname.isBlank()) {
            throw new IllegalArgumentException("surname must not be blank");
        }
        if (pictureUrl != null && pictureUrl.isBlank()) {
            throw new IllegalArgumentException("pictureUrl must not be blank");
        }
    }
}
