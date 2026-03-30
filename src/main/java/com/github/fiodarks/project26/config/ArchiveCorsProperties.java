package com.github.fiodarks.project26.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "archive.cors")
public record ArchiveCorsProperties(List<String> allowedOrigins) {
    public ArchiveCorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
                .distinct()
                .toList();
    }
}
