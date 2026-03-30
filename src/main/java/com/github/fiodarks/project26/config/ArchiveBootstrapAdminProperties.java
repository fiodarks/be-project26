package com.github.fiodarks.project26.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "archive.security.bootstrap")
public record ArchiveBootstrapAdminProperties(List<String> adminEmails) {
    public ArchiveBootstrapAdminProperties {
        adminEmails = List.copyOf(Objects.requireNonNullElse(adminEmails, List.of()));
    }
}

