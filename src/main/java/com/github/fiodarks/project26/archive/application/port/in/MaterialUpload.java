package com.github.fiodarks.project26.archive.application.port.in;

import java.io.InputStream;
import java.util.Objects;

public record MaterialUpload(
        String originalFilename,
        String contentType,
        long sizeBytes,
        InputStream inputStream
) {
    public MaterialUpload {
        Objects.requireNonNull(originalFilename, "originalFilename");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(inputStream, "inputStream");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must be >= 0");
        }
    }
}

