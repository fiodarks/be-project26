package com.github.fiodarks.project26.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "archive.security.google")
public record GoogleOAuthProperties(
        boolean enabled,
        String clientId,
        String clientSecret,
        String redirectUri
) {
    public GoogleOAuthProperties {
        if (enabled) {
            if (clientId == null || clientId.isBlank()) {
                throw new IllegalArgumentException("archive.security.google.client-id must be set when Google OAuth is enabled");
            }
            if (clientSecret == null || clientSecret.isBlank()) {
                throw new IllegalArgumentException("archive.security.google.client-secret must be set when Google OAuth is enabled");
            }
            if (redirectUri == null || redirectUri.isBlank()) {
                throw new IllegalArgumentException("archive.security.google.redirect-uri must be set when Google OAuth is enabled");
            }
        }
    }
}
