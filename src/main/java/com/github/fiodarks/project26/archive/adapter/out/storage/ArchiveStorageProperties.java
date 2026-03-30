package com.github.fiodarks.project26.archive.adapter.out.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.Objects;

@ConfigurationProperties(prefix = "archive.storage")
public record ArchiveStorageProperties(
        Path baseDir,
        String publicBaseUrl
) {
    public ArchiveStorageProperties {
        Objects.requireNonNull(baseDir, "baseDir");
    }
}
