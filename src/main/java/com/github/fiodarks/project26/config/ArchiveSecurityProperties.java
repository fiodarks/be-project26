package com.github.fiodarks.project26.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "archive.security.jwt")
public record ArchiveSecurityProperties(String hmacSecret) {
    public ArchiveSecurityProperties {
        if (hmacSecret == null || hmacSecret.isBlank()) {
            throw new IllegalArgumentException("archive.security.jwt.hmac-secret must be set");
        }
    }
}

