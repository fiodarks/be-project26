package com.github.fiodarks.project26.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "archive.cors")
public record ArchiveCorsProperties(List<String> allowedOrigins, List<String> allowedOriginPatterns) {
    public ArchiveCorsProperties {
        allowedOrigins = normalize(allowedOrigins);
        allowedOriginPatterns = normalize(allowedOriginPatterns);
    }

    private static List<String> normalize(List<String> values) {
        return values == null
                ? List.of()
                : values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
                .distinct()
                .toList();
    }
}
